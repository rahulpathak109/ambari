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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.ambari.server.ldap.domain.AmbariLdapConfigKeys;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.service.AmbariLdapException;
import org.apache.ambari.server.ldap.service.LdapAttributeDetectionService;
import org.apache.ambari.server.ldap.service.ads.detectors.GroupMemberAttrDetector;
import org.apache.ambari.server.ldap.service.ads.detectors.GroupNameAttrDetector;
import org.apache.ambari.server.ldap.service.ads.detectors.GroupObjectClassDetector;
import org.apache.ambari.server.ldap.service.ads.detectors.UserGroupMemberAttrDetector;
import org.apache.ambari.server.ldap.service.ads.detectors.UserNameAttrDetector;
import org.apache.ambari.server.ldap.service.ads.detectors.UserObjectClassDetector;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.util.Strings;
import org.apache.directory.ldap.client.api.search.FilterBuilder;
import org.apache.directory.ldap.client.template.EntryMapper;
import org.apache.directory.ldap.client.template.LdapConnectionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DefaultLdapAttributeDetectionService implements LdapAttributeDetectionService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLdapAttributeDetectionService.class);
  private static final int SAMPLE_RESULT_SIZE = 50;

  @Inject
  private UserNameAttrDetector userNameAttrDetector;

  @Inject
  private UserObjectClassDetector userObjectClassDetector;

  @Inject
  private UserGroupMemberAttrDetector userGroupMemberAttrDetector;

  @Inject
  private GroupNameAttrDetector groupNameAttrDetector;

  @Inject
  private GroupObjectClassDetector groupObjectClassDetector;

  @Inject
  private GroupMemberAttrDetector groupMemberAttrDetector;

  @Inject
  private LdapConnectionTemplateFactory ldapConnectionTemplateFactory;


  @Inject
  public DefaultLdapAttributeDetectionService() {
  }

  @Override
  public AmbariLdapConfiguration detectLdapUserAttributes(AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException {
    LOGGER.info("Detecting LDAP user attributes ...");
    LdapConnectionTemplate ldapConnectionTemplate = ldapConnectionTemplateFactory.create(ambariLdapConfiguration);


    // perform a search using the user search base
    if (Strings.isEmpty(ambariLdapConfiguration.userSearchBase())) {
      LOGGER.warn("No user search base provided");
      return ambariLdapConfiguration;
    }

    try {

      SearchRequest searchRequest = assembleUserSearchRequest(ldapConnectionTemplate, ambariLdapConfiguration);

      // do the search
      List<Entry> entries = ldapConnectionTemplate.search(searchRequest, getEntryMapper());

      for (Entry entry : entries) {

        LOGGER.info("Processing sample entry with dn: [{}]", entry.getDn());

        userNameAttrDetector.collect(entry);
        userObjectClassDetector.collect(entry);
        userGroupMemberAttrDetector.collect(entry);

      }

      ambariLdapConfiguration.setValueFor(AmbariLdapConfigKeys.USER_NAME_ATTRIBUTE, userNameAttrDetector.detect());
      ambariLdapConfiguration.setValueFor(AmbariLdapConfigKeys.USER_OBJECT_CLASS, userObjectClassDetector.detect());
      ambariLdapConfiguration.setValueFor(AmbariLdapConfigKeys.USER_GROUP_MEMBER_ATTRIBUTE, userGroupMemberAttrDetector.detect());

      LOGGER.info("Decorated ambari ldap config : [{}]", ambariLdapConfiguration);

    } catch (Exception e) {
      LOGGER.error("Ldap operation failed", e);
    }

    return ambariLdapConfiguration;
  }


  @Override
  public AmbariLdapConfiguration detectLdapGroupAttributes(AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException {
    LOGGER.info("Detecting LDAP group attributes ...");

    // perform a search using the user search base
    if (Strings.isEmpty(ambariLdapConfiguration.groupSearchBase())) {
      LOGGER.warn("No group search base provided");
      return ambariLdapConfiguration;
    }

    LdapConnectionTemplate ldapConnectionTemplate = ldapConnectionTemplateFactory.create(ambariLdapConfiguration);

    try {

      SearchRequest searchRequest = assembleGroupSearchRequest(ldapConnectionTemplate, ambariLdapConfiguration);

      // do the search
      List<Entry> groupEntries = ldapConnectionTemplate.search(searchRequest, getEntryMapper());


      for (Entry groupEntry : groupEntries) {

        LOGGER.info("Processing sample entry with dn: [{}]", groupEntry.getDn());
        groupNameAttrDetector.collect(groupEntry);
        groupObjectClassDetector.collect(groupEntry);
        groupMemberAttrDetector.collect(groupEntry);

      }

      ambariLdapConfiguration.setValueFor(AmbariLdapConfigKeys.GROUP_NAME_ATTRIBUTE, groupNameAttrDetector.detect());
      ambariLdapConfiguration.setValueFor(AmbariLdapConfigKeys.GROUP_OBJECT_CLASS, groupObjectClassDetector.detect());
      ambariLdapConfiguration.setValueFor(AmbariLdapConfigKeys.GROUP_MEMBER_ATTRIBUTE, groupMemberAttrDetector.detect());

      LOGGER.info("Decorated ambari ldap config : [{}]", ambariLdapConfiguration);

    } catch (Exception e) {

      LOGGER.error("Ldap operation failed", e);
    }

    return ambariLdapConfiguration;
  }

  private SearchRequest assembleUserSearchRequest(LdapConnectionTemplate ldapConnectionTemplate, AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException {
    try {

      SearchRequest req = ldapConnectionTemplate.newSearchRequest(ambariLdapConfiguration.userSearchBase(),
        FilterBuilder.present(ambariLdapConfiguration.dnAttribute()).toString(), SearchScope.SUBTREE);
      req.setSizeLimit(SAMPLE_RESULT_SIZE);

      return req;

    } catch (Exception e) {
      LOGGER.error("Could not assemble ldap search request", e);
      throw new AmbariLdapException(e);
    }
  }

  private SearchRequest assembleGroupSearchRequest(LdapConnectionTemplate ldapConnectionTemplate, AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException {
    try {

      SearchRequest req = ldapConnectionTemplate.newSearchRequest(ambariLdapConfiguration.groupSearchBase(),
        FilterBuilder.present(ambariLdapConfiguration.dnAttribute()).toString(), SearchScope.SUBTREE);
      req.setSizeLimit(SAMPLE_RESULT_SIZE);

      return req;

    } catch (Exception e) {
      LOGGER.error("Could not assemble ldap search request", e);
      throw new AmbariLdapException(e);
    }
  }


  public EntryMapper<Entry> getEntryMapper() {
    return new EntryMapper<Entry>() {
      @Override
      public Entry map(Entry entry) throws LdapException {
        return entry;
      }
    };
  }
}
