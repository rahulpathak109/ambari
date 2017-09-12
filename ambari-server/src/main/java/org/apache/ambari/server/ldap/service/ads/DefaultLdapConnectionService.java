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

import javax.inject.Singleton;

import org.apache.ambari.server.ldap.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.service.LdapConnectionService;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DefaultLdapConnectionService implements LdapConnectionService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLdapConnectionService.class);

  @Override
  public LdapNetworkConnection createLdapConnection(AmbariLdapConfiguration ambariLdapConfiguration) {
    LOGGER.debug("Creating ldap connection instance from: {}", ambariLdapConfiguration);

    return new LdapNetworkConnection(getLdapConnectionConfig(ambariLdapConfiguration));
  }

  @Override
  public LdapConnection getBoundLdapConnection(AmbariLdapConfiguration ambariLdapConfiguration) {
    LOGGER.info("Creating LDAP connection instance and binding to LDAP server ...");

    try {
      LdapConnection connection = createLdapConnection(ambariLdapConfiguration);

      if (!ambariLdapConfiguration.anonymousBind()) {

        LOGGER.debug("Anonymous binding not supported, binding with the manager detailas...");
        connection.bind(ambariLdapConfiguration.bindDn(), ambariLdapConfiguration.bindPassword());

      } else {

        LOGGER.debug("Binding anonymously ...");
        connection.bind();

      }

      if (!connection.isConnected()) {

        LOGGER.error("Not connected to the LDAP server. Connection instance: {}", connection);
        throw new IllegalStateException("The connection to the LDAP server is not alive");

      }

      LOGGER.info("Connected / bound to LDAP server.");
      return connection;

    } catch (Exception e) {
      LOGGER.error("Could not create or bind LdapConnection", e);
      throw new IllegalArgumentException(e);
    }

  }

  private LdapConnectionConfig getLdapConnectionConfig(AmbariLdapConfiguration ambariAmbariLdapConfiguration) {
    LOGGER.debug("Creating a LDAP connection config instance based on the ambari configuration: {}", ambariAmbariLdapConfiguration);

    LdapConnectionConfig ldapConnectionConfig = new LdapConnectionConfig();
    ldapConnectionConfig.setLdapHost(ambariAmbariLdapConfiguration.serverHost());
    ldapConnectionConfig.setLdapPort(ambariAmbariLdapConfiguration.serverPort());
    ldapConnectionConfig.setUseSsl(ambariAmbariLdapConfiguration.useSSL());

    return ldapConnectionConfig;
  }

}
