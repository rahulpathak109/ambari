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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.directory.ldap.client.api.DefaultLdapConnectionFactory;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapConnectionFactory;
import org.apache.directory.ldap.client.api.LdapConnectionPool;
import org.apache.directory.ldap.client.api.ValidatingPoolableLdapConnectionFactory;
import org.apache.directory.ldap.client.template.LdapConnectionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating LdapConnectionTemplate instances.
 * Depending on the usage context, the instance can be constructed based on the provided configuration or based on the persisted settings.
 */
@Singleton
public class LdapConnectionTemplateFactory {

  private static final Logger LOG = LoggerFactory.getLogger(LdapConnectionTemplateFactory.class);

  @Inject
  private Provider<LdapConnectionTemplate> ldapConnectionTemplate;

  @Inject
  public LdapConnectionTemplateFactory() {
  }

  /**
   * Creates a new instance based on the provided configuration. Use this factory method whle operating with ambari configuration not yet persisted.
   *
   * @param ambariLdapConfiguration ambari ldap configuration instance
   * @return an instance of LdapConnectionTemplate
   */
  public LdapConnectionTemplate create(AmbariLdapConfiguration ambariLdapConfiguration) {
    LOG.info("Constructing new instance based on the provided ambari ldap configuration: {}", ambariLdapConfiguration);

    // create the connection config
    LdapConnectionConfig ldapConnectionConfig = getLdapConnectionConfig(ambariLdapConfiguration);

    // create the connection factory
    LdapConnectionFactory ldapConnectionFactory = new DefaultLdapConnectionFactory(ldapConnectionConfig);

    // create the connection pool
    LdapConnectionPool ldapConnectionPool = new LdapConnectionPool(new ValidatingPoolableLdapConnectionFactory(ldapConnectionFactory));

    LdapConnectionTemplate template = new LdapConnectionTemplate(ldapConnectionPool);
    LOG.info("Ldap connection template instance: {}", template);

    return template;

  }

  public LdapConnectionTemplate load() {
    // the construction logic is implemented in the provider class
    return ldapConnectionTemplate.get();
  }


  private LdapConnectionConfig getLdapConnectionConfig(AmbariLdapConfiguration ambariLdapConfiguration) {

    LdapConnectionConfig config = new LdapConnectionConfig();
    config.setLdapHost(ambariLdapConfiguration.serverHost());
    config.setLdapPort(ambariLdapConfiguration.serverPort());
    config.setName(ambariLdapConfiguration.bindDn());
    config.setCredentials(ambariLdapConfiguration.bindPassword());

    // todo set the other required properties here, eg.: trustmanager
    return config;
  }

  private LdapConnectionFactory getLdapConnectionFactory(AmbariLdapConfiguration ambariLdapConfiguration) {
    return new DefaultLdapConnectionFactory(getLdapConnectionConfig(ambariLdapConfiguration));
  }


}
