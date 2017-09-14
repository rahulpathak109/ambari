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

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AmbariLdapFacade implements LdapFacade {

  private static final Logger LOGGER = LoggerFactory.getLogger(AmbariLdapFacade.class);

  /**
   * Additional parameters expected to be provided along with the configuration
   */
  private enum Parameters {
    TEST_USER_NAME("ambari.ldap.test.user.name"),
    TEST_USER_PASSWORD("ambari.ldap.test.user.password");

    private String parameterKey;

    Parameters(String parameterKey) {
      this.parameterKey = parameterKey;
    }

    private String getParameterKey() {
      return parameterKey;
    }

  }

  @Inject
  private LdapConfigurationService ldapConfigurationService;

  @Inject
  private LdapConnectionService ldapConnectionService;

  @Inject
  private LdapAttributeDetectionService ldapAttributeDetectionService;

  @Inject
  public AmbariLdapFacade() {
  }

  @Override
  public void checkConnection(AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException {
    LdapConnection connection = null;
    try {

      LOGGER.info("Validating LDAP connection related configuration based on: {}", ambariLdapConfiguration);
      connection = ldapConnectionService.getBoundLdapConnection(ambariLdapConfiguration);

      ldapConfigurationService.checkConnection(connection, ambariLdapConfiguration);
      LOGGER.info("Validating LDAP connection related configuration: SUCCESS");

    } catch (Exception e) {

      LOGGER.error("Validating LDAP connection configuration failed", e);
      throw new AmbariLdapException(e);

    } finally {
      try {
        connection.unBind();
        connection.close();
      } catch (Exception e) {
        throw new AmbariLdapException(e);
      }
    }

  }


  @Override
  public AmbariLdapConfiguration detectAttributes(AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException {
    LOGGER.info("Detecting LDAP configuration attributes ...");

    LdapConnection connection = ldapConnectionService.getBoundLdapConnection(ambariLdapConfiguration);
    try {

      // decorate the configuration with detected user attributes
      ambariLdapConfiguration = ldapAttributeDetectionService.detectLdapUserAttributes(connection, ambariLdapConfiguration);

      // decorate the configuration with detected group attributes
      ambariLdapConfiguration = ldapAttributeDetectionService.detectLdapGroupAttributes(connection, ambariLdapConfiguration);
      return ambariLdapConfiguration;

    } catch (Exception e) {

      LOGGER.error("Error during LDAP attribute detection", e);
      throw new AmbariLdapException(e);

    } finally {
      try {
        connection.unBind();
        connection.close();
      } catch (Exception e) {
        throw new AmbariLdapException(e);
      }
    }
  }

  @Override
  public Set<String> checkLdapAttributes(Map<String, Object> parameters, AmbariLdapConfiguration ldapConfiguration) throws AmbariLdapException {
    String userName = getTestUserNameFromParameters(parameters);
    String testUserPass = getTestUserPasswordFromParameters(parameters);

    if (null == userName) {
      throw new IllegalArgumentException("No test user available for testing LDAP attributes");
    }

    LdapConnection ldapConnection = ldapConnectionService.getBoundLdapConnection(ldapConfiguration);

    LOGGER.info("Testing LDAP user attributes with test user: {}", userName);
    String userDn = ldapConfigurationService.checkUserAttributes(ldapConnection, userName, testUserPass, ldapConfiguration);

    // todo handle the case where group membership is stored in the user rather than the group
    LOGGER.info("Testing LDAP group attributes with test user dn: {}", userDn);
    Set<String> groups = ldapConfigurationService.checkGroupAttributes(ldapConnection, userDn, ldapConfiguration);

    return groups;
  }


  private String getTestUserNameFromParameters(Map<String, Object> parameters) {
    return (String) parameterValue(parameters, Parameters.TEST_USER_NAME);
  }

  private String getTestUserPasswordFromParameters(Map<String, Object> parameters) {
    return (String) parameterValue(parameters, Parameters.TEST_USER_PASSWORD);
  }

  private Object parameterValue(Map<String, Object> parameters, Parameters parameter) {
    Object value = null;
    if (parameters.containsKey(parameter.getParameterKey())) {
      value = parameters.get(parameter.getParameterKey());
    } else {
      LOGGER.warn("Parameter [{}] is missing from parameters", parameter.getParameterKey());
    }
    return value;
  }
}
