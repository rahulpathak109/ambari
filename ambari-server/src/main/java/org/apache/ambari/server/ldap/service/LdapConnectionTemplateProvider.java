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

package org.apache.ambari.server.ldap.service;


import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.directory.ldap.client.api.DefaultLdapConnectionFactory;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapConnectionFactory;
import org.apache.directory.ldap.client.api.LdapConnectionPool;
import org.apache.directory.ldap.client.api.ValidatingPoolableLdapConnectionFactory;
import org.apache.directory.ldap.client.template.LdapConnectionTemplate;

public class LdapConnectionTemplateProvider implements Provider<LdapConnectionTemplate> {

  // Inject the persisted configuration (when available) check the provider implementation for details.
  @Inject
  private Provider<AmbariLdapConfiguration> ambariLdapConfigurationProvider;

  @Override
  public LdapConnectionTemplate get() {
    return new LdapConnectionTemplate(new LdapConnectionPool(
      new ValidatingPoolableLdapConnectionFactory(getLdapConnectionFactory())));
  }

  private LdapConnectionConfig getLdapConnectionConfig() {
    LdapConnectionConfig config = new LdapConnectionConfig();
    config.setLdapHost(ambariLdapConfigurationProvider.get().serverHost());
    config.setLdapPort(ambariLdapConfigurationProvider.get().serverPort());
    config.setName(ambariLdapConfigurationProvider.get().bindDn());
    config.setCredentials(ambariLdapConfigurationProvider.get().bindPassword());

    return config;
  }

  private LdapConnectionFactory getLdapConnectionFactory() {
    return new DefaultLdapConnectionFactory(getLdapConnectionConfig());
  }


}
