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

import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfigurationFactory;
import org.apache.ambari.server.ldap.service.AmbariLdapConfigurationProvider;
import org.apache.ambari.server.ldap.service.AmbariLdapFacade;
import org.apache.ambari.server.ldap.service.LdapAttributeDetectionService;
import org.apache.ambari.server.ldap.service.LdapConfigurationService;
import org.apache.ambari.server.ldap.service.LdapConnectionTemplateProvider;
import org.apache.ambari.server.ldap.service.LdapFacade;
import org.apache.ambari.server.ldap.service.ads.DefaultLdapAttributeDetectionService;
import org.apache.ambari.server.ldap.service.ads.DefaultLdapConfigurationService;
import org.apache.directory.ldap.client.template.LdapConnectionTemplate;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

/**
 * GUICE configuration module for setting up LDAP related infrastructure.
 */
public class LdapModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(LdapFacade.class).to(AmbariLdapFacade.class);
    bind(LdapConfigurationService.class).to(DefaultLdapConfigurationService.class);
    bind(LdapAttributeDetectionService.class).to(DefaultLdapAttributeDetectionService.class);

    // this binding requires the JPA module!
    bind(AmbariLdapConfiguration.class).toProvider(AmbariLdapConfigurationProvider.class);

    // bind to the provider implementation (let GUICE deal with instantiating 3rd party instances)
    bind(LdapConnectionTemplate.class).toProvider(LdapConnectionTemplateProvider.class);

    install(new FactoryModuleBuilder().build(AmbariLdapConfigurationFactory.class));
  }
}
