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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

/**
 * This class is an immutable representation of all the LDAP related configurationMap entries.
 */
@Singleton
public class AmbariLdapConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(AmbariLdapConfiguration.class);

  /**
   * Constants representing supported LDAP related property names
   */
  public enum AmbariLdapConfig {

    LDAP_ENABLED("ambari.ldap.authentication.enabled"),
    SERVER_HOST("ambari.ldap.connectivity.server.host"),
    SERVER_PORT("ambari.ldap.connectivity.server.port"),
    USE_SSL("ambari.ldap.connectivity.use_ssl"),

    TRUST_STORE("ambari.ldap.connectivity.trust_store"),
    TRUST_STORE_TYPE("ambari.ldap.connectivity.trust_store.type"),
    TRUST_STORE_PATH("ambari.ldap.connectivity.trust_store.path"),
    TRUST_STORE_PASSWORD("ambari.ldap.connectivity.trust_store.password"),
    ANONYMOUS_BIND("ambari.ldap.connectivity.anonymous_bind"),

    BIND_DN("ambari.ldap.connectivity.bind_dn"),
    BIND_PASSWORD("ambari.ldap.connectivity.bind_password"),

    ATTR_DETECTION("ambari.ldap.attributes.detection"), // manual | auto

    DN_ATTRIBUTE("ambari.ldap.attributes.dn_attr"),

    USER_OBJECT_CLASS("ambari.ldap.attributes.user.object_class"),
    USER_NAME_ATTRIBUTE("ambari.ldap.attributes.user.name_attr"),
    USER_SEARCH_BASE("ambari.ldap.attributes.user.search_base"),

    GROUP_OBJECT_CLASS("ambari.ldap.attributes.group.object_class"),
    GROUP_NAME_ATTRIBUTE("ambari.ldap.attributes.group.name_attr"),
    GROUP_MEMBER_ATTRIBUTE("ambari.ldap.attributes.group.member_attr"),
    GROUP_SEARCH_BASE("ambari.ldap.attributes.user.search_base"),

    USER_SEARCH_FILTER("ambari.ldap.advanced.user_search_filter"),
    USER_MEMBER_REPLACE_PATTERN("ambari.ldap.advanced.user_member_replace_pattern"),
    USER_MEMBER_FILTER("ambari.ldap.advanced.user_member_filter"),

    GROUP_SEARCH_FILTER("ambari.ldap.advanced.group_search_filter"),
    GROUP_MEMBER_REPLACE_PATTERN("ambari.ldap.advanced.group_member_replace_pattern"),
    GROUP_MEMBER_FILTER("ambari.ldap.advanced.group_member_filter"),

    FORCE_LOWERCASE_USERNAMES("ambari.ldap.advanced.force_lowercase_usernames"),
    REFERRAL_HANDLING("ambari.ldap.advanced.referrals"), // folow
    PAGINATION_ENABLED("ambari.ldap.advanced.pagination_enabled"); // true | false

    private String propertyName;

    AmbariLdapConfig(String propName) {
      this.propertyName = propName;
    }

    public String key() {
      return this.propertyName;
    }
  }

  private final Map<String, Object> configurationMap;

  private Object configValue(AmbariLdapConfig ambariLdapConfig) {
    Object value = null;
    if (configurationMap.containsKey(ambariLdapConfig.key())) {
      value = configurationMap.get(ambariLdapConfig.key());
    } else {
      LOGGER.warn("Ldap configuration property [{}] hasn't been set", ambariLdapConfig.key());
    }
    return value;
  }

  @Inject
  public AmbariLdapConfiguration(@Assisted Map<String, Object> configuration) {
    this.configurationMap = configuration;
  }

  public boolean ldapEnabled() {
    return Boolean.valueOf((String) configValue(AmbariLdapConfig.LDAP_ENABLED));
  }

  public String serverHost() {
    return (String) configValue(AmbariLdapConfig.SERVER_HOST);
  }

  public int serverPort() {
    return Integer.valueOf((String) configValue(AmbariLdapConfig.SERVER_PORT));
  }

  public boolean useSSL() {
    return Boolean.valueOf((String) configValue(AmbariLdapConfig.USE_SSL));
  }

  public String trustStore() {
    return (String) configValue(AmbariLdapConfig.TRUST_STORE);
  }

  public String trustStoreType() {
    return (String) configValue(AmbariLdapConfig.TRUST_STORE_TYPE);
  }

  public String trustStorePath() {
    return (String) configValue(AmbariLdapConfig.TRUST_STORE_PATH);
  }

  public String trustStorePassword() {
    return (String) configValue(AmbariLdapConfig.TRUST_STORE_PASSWORD);
  }

  public boolean anonymousBind() {
    return Boolean.valueOf((String) configValue(AmbariLdapConfig.ANONYMOUS_BIND));
  }

  public String bindDn() {
    return (String) configValue(AmbariLdapConfig.BIND_DN);
  }

  public String bindPassword() {
    return (String) configValue(AmbariLdapConfig.BIND_PASSWORD);
  }

  public String attributeDetection() {
    return (String) configValue(AmbariLdapConfig.ATTR_DETECTION);
  }

  public String dnAttribute() {
    return (String) configValue(AmbariLdapConfig.DN_ATTRIBUTE);
  }

  public String userObjectClass() {
    return (String) configValue(AmbariLdapConfig.USER_OBJECT_CLASS);
  }

  public String userNameAttribute() {
    return (String) configValue(AmbariLdapConfig.USER_NAME_ATTRIBUTE);
  }

  public String userSearchBase() {
    return (String) configValue(AmbariLdapConfig.USER_SEARCH_BASE);
  }

  public String groupObjectClass() {
    return (String) configValue(AmbariLdapConfig.GROUP_OBJECT_CLASS);
  }

  public String groupNameAttribute() {
    return (String) configValue(AmbariLdapConfig.GROUP_NAME_ATTRIBUTE);
  }

  public String groupMemberAttribute() {
    return (String) configValue(AmbariLdapConfig.GROUP_MEMBER_ATTRIBUTE);
  }

  public String groupSearchBase() {
    return (String) configValue(AmbariLdapConfig.GROUP_SEARCH_BASE);
  }

  public String userSearchFilter() {
    return (String) configValue(AmbariLdapConfig.USER_SEARCH_FILTER);
  }

  public String userMemberReplacePattern() {
    return (String) configValue(AmbariLdapConfig.USER_MEMBER_REPLACE_PATTERN);
  }

  public String userMemberFilter() {
    return (String) configValue(AmbariLdapConfig.USER_MEMBER_FILTER);
  }

  public String groupSearchFilter() {
    return (String) configValue(AmbariLdapConfig.GROUP_SEARCH_FILTER);
  }

  public String groupMemberReplacePattern() {
    return (String) configValue(AmbariLdapConfig.GROUP_MEMBER_REPLACE_PATTERN);
  }

  public String groupMemberFilter() {
    return (String) configValue(AmbariLdapConfig.GROUP_MEMBER_FILTER);
  }

  public boolean forceLowerCaseUserNames() {
    return Boolean.valueOf((String) configValue(AmbariLdapConfig.FORCE_LOWERCASE_USERNAMES));
  }

  public boolean paginationEnabled() {
    return Boolean.valueOf((String) configValue(AmbariLdapConfig.PAGINATION_ENABLED));
  }

  public String referralHandling() {
    return (String) configValue(AmbariLdapConfig.REFERRAL_HANDLING);
  }

}
