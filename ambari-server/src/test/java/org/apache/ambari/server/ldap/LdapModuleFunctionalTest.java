/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap;

import java.util.Map;

import org.apache.ambari.server.ldap.domain.AmbariLdapConfigKeys;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.domain.TestAmbariAmbariLdapConfigurationFactory;
import org.apache.ambari.server.ldap.service.LdapConfigurationService;
import org.apache.ambari.server.ldap.service.LdapFacade;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.template.ConnectionCallback;
import org.apache.directory.ldap.client.template.LdapConnectionTemplate;
import org.apache.directory.ldap.client.template.exception.PasswordException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

public class LdapModuleFunctionalTest {

  private static Injector injector;
  private static Module testModule;
  private static TestAmbariAmbariLdapConfigurationFactory ldapConfigurationFactory = new TestAmbariAmbariLdapConfigurationFactory();

  @BeforeClass
  public static void beforeClass() throws Exception {

    // overriding bindings for testing purposes
    testModule = Modules.override(new LdapModule()).with(new AbstractModule() {
      @Override
      protected void configure() {
        // override the configuration instance binding not to access the database
        bind(AmbariLdapConfiguration.class).toInstance(ldapConfigurationFactory.createLdapConfiguration(getProps()));
      }
    });

    injector = Guice.createInjector(testModule);
  }

  @Test
  public void shouldLdapTemplateBeInstantiated() throws LdapInvalidDnException, PasswordException {
    // GIVEN
    // the injector is set up
    Assert.assertNotNull(injector);

    // WHEN
    // the ldap connection template is retrieved
    LdapConnectionTemplate template = injector.getInstance(LdapConnectionTemplate.class);

    // THEN
    Assert.assertNotNull(template);
    template.authenticate(new Dn("cn=read-only-admin,dc=example,dc=com"), "password".toCharArray());

    Boolean success = template.execute(new ConnectionCallback<Boolean>() {
      @Override
      public Boolean doWithConnection(LdapConnection connection) throws LdapException {
        connection.unBind();
        connection.bind(new Dn("cn=read-only-admin,dc=example,dc=com"), "password");
        return connection.isConnected() && connection.isAuthenticated();
      }
    });

    Assert.assertTrue("Could not bind to the LDAP server", success);

  }


  @Test
  public void testShouldConnectionCheckSucceedWhenProperConfigurationProvided() throws Exception {
    // GIVEN
    AmbariLdapConfiguration ambariLdapConfiguration = ldapConfigurationFactory.createLdapConfiguration(getProps());

    LdapFacade ldapFacade = injector.getInstance(LdapFacade.class);


    // WHEN
    ldapFacade.checkConnection(ambariLdapConfiguration);

    ldapFacade.detectAttributes(ambariLdapConfiguration);

    // THEN
    // no exceptions thrown

  }

  @Test
  public void testShouldAttributeDetectionSucceedWhenProperConfigurationProvided() throws Exception {
    // GIVEN
    AmbariLdapConfiguration ambariLdapConfiguration = ldapConfigurationFactory.createLdapConfiguration(getProps());
    LdapConfigurationService ldapConfigurationService = injector.getInstance(LdapConfigurationService.class);


    // WHEN
    ldapConfigurationService.checkUserAttributes("euclid", "", ambariLdapConfiguration);

    // THEN
    // no exceptions thrown

  }

  private static Map<String, Object> getProps() {
    Map<String, Object> ldapPropsMap = Maps.newHashMap();

    ldapPropsMap.put(AmbariLdapConfigKeys.ANONYMOUS_BIND.key(), "true");
    ldapPropsMap.put(AmbariLdapConfigKeys.SERVER_HOST.key(), "ldap.forumsys.com");
    ldapPropsMap.put(AmbariLdapConfigKeys.SERVER_PORT.key(), "389");
    ldapPropsMap.put(AmbariLdapConfigKeys.BIND_DN.key(), "cn=read-only-admin,dc=example,dc=com");
    ldapPropsMap.put(AmbariLdapConfigKeys.BIND_PASSWORD.key(), "password");

    ldapPropsMap.put(AmbariLdapConfigKeys.USER_OBJECT_CLASS.key(), SchemaConstants.PERSON_OC);
    ldapPropsMap.put(AmbariLdapConfigKeys.USER_NAME_ATTRIBUTE.key(), SchemaConstants.UID_AT);
    ldapPropsMap.put(AmbariLdapConfigKeys.USER_SEARCH_BASE.key(), "dc=example,dc=com");
    ldapPropsMap.put(AmbariLdapConfigKeys.DN_ATTRIBUTE.key(), SchemaConstants.UID_AT);


    return ldapPropsMap;
  }

}