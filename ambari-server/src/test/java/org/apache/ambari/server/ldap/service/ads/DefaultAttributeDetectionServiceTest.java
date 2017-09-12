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

package org.apache.ambari.server.ldap.service.ads;


import java.util.Map;

import org.apache.ambari.server.ldap.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.service.LdapConnectionService;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.easymock.EasyMockRule;
import org.easymock.TestSubject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class DefaultAttributeDetectionServiceTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAttributeDetectionServiceTest.class);

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  private AmbariLdapConfiguration testLdapConfiguration;
  private LdapConnection connection;

  @TestSubject
  private DefaultAttributeDetectionService attributeDetectionService = new DefaultAttributeDetectionService();

  @Before
  public void before() {

    Map<String, Object> initialProps = Maps.newHashMap();
    initialProps.put(AmbariLdapConfiguration.AmbariLdapConfig.BIND_DN.key(), "");
    testLdapConfiguration = new AmbariLdapConfiguration(initialProps);
  }

  @Test
  public void testShouldUserNameAttributeBeDetectedWhenSearchReturnsValidUsers() throws Exception {
    // GIVEN
// a set of entries returned from the LDAP search

    // WHEN
    AmbariLdapConfiguration ambariLdapConfiguration = attributeDetectionService.detectLdapUserAttributes(connection, testLdapConfiguration);

    // THEN
    Assert.assertNotNull(ambariLdapConfiguration);
    Assert.assertEquals("The username attribute is not the expected", "uid", ambariLdapConfiguration.userNameAttribute());

  }


  @Test
  public void functionalTest() throws Exception {
    // GIVEN
    AmbariLdapConfiguration ambariLdapConfiguration = new AmbariLdapConfiguration(getTestPropertiesMap());
    LdapConnectionService connectionService = new DefaultLdapConnectionService();
    LdapNetworkConnection ldapConnection = connectionService.createLdapConnection(ambariLdapConfiguration);

    // WHEN
    AmbariLdapConfiguration config = attributeDetectionService.detectLdapUserAttributes(ldapConnection, ambariLdapConfiguration);
    config = attributeDetectionService.detectLdapGroupAttributes(ldapConnection, ambariLdapConfiguration);

    Gson gson = new GsonBuilder().create();
    LOGGER.info(gson.toJson(config));

    // THEN
    ldapConnection.close();

  }

  private Map<String, Object> getTestPropertiesMap() {
    Map<String, Object> ldapPropsMap = Maps.newHashMap();

    ldapPropsMap.put(AmbariLdapConfiguration.AmbariLdapConfig.ANONYMOUS_BIND.key(), "true");
    ldapPropsMap.put(AmbariLdapConfiguration.AmbariLdapConfig.SERVER_HOST.key(), "ldap.forumsys.com");
    ldapPropsMap.put(AmbariLdapConfiguration.AmbariLdapConfig.SERVER_PORT.key(), "389");
    ldapPropsMap.put(AmbariLdapConfiguration.AmbariLdapConfig.BIND_DN.key(), "cn=read-only-admin,dc=example,dc=com");
    ldapPropsMap.put(AmbariLdapConfiguration.AmbariLdapConfig.BIND_PASSWORD.key(), "password");
    ldapPropsMap.put(AmbariLdapConfiguration.AmbariLdapConfig.DN_ATTRIBUTE.key(), SchemaConstants.CN_AT);

    ldapPropsMap.put(AmbariLdapConfiguration.AmbariLdapConfig.USER_SEARCH_BASE.key(), "dc=example,dc=com");
    ldapPropsMap.put(AmbariLdapConfiguration.AmbariLdapConfig.GROUP_SEARCH_BASE.key(), "dc=example,dc=com");

    return ldapPropsMap;

  }
}