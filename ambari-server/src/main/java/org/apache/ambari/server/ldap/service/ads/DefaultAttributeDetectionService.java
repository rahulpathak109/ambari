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
import javax.inject.Singleton;

import org.apache.ambari.server.ldap.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.service.LdapAttributeDetectionService;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DefaultAttributeDetectionService implements LdapAttributeDetectionService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAttributeDetectionService.class);

  @Inject
  public DefaultAttributeDetectionService() {
  }

  @Override
  public AmbariLdapConfiguration detectLdapUserAttributes(LdapConnection connection, AmbariLdapConfiguration ambariLdapConfiguration) {
    LOGGER.info("Detecting LDAP user attributes ...");

    return null;
  }

  @Override
  public AmbariLdapConfiguration detectLdapGroupAttributes(LdapConnection connection, AmbariLdapConfiguration ambariLdapConfiguration) {
    LOGGER.info("Detecting LDAP group attributes ...");
    return null;
  }
}
