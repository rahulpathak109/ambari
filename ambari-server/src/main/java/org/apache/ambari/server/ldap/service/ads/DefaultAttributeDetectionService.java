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
import org.apache.ambari.server.ldap.service.AmbariLdapException;
import org.apache.ambari.server.ldap.service.LdapAttributeDetectionService;
import org.apache.ambari.server.ldap.service.ads.detectors.GroupMemberAttrDetector;
import org.apache.ambari.server.ldap.service.ads.detectors.GroupNameAttrDetector;
import org.apache.ambari.server.ldap.service.ads.detectors.GroupObjectClassDetector;
import org.apache.ambari.server.ldap.service.ads.detectors.UserGroupMemberAttrDetector;
import org.apache.ambari.server.ldap.service.ads.detectors.UserNameAttrDetector;
import org.apache.ambari.server.ldap.service.ads.detectors.UserObjectClassDetector;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.Strings;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.search.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DefaultAttributeDetectionService implements LdapAttributeDetectionService<LdapConnection> {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAttributeDetectionService.class);
  private static final int SAMPLE_RESULT_SIZE = 50;


  @Inject
  private UserNameAttrDetector userNameAttrDetector = new UserNameAttrDetector(); // todo remove instantition

  @Inject
  private UserObjectClassDetector userObjectClassDetector = new UserObjectClassDetector(); // todo remove instantition

  @Inject
  private UserGroupMemberAttrDetector userGroupMemberAttrDetector = new UserGroupMemberAttrDetector(); // todo remove instantition

  @Inject
  private GroupNameAttrDetector groupNameAttrDetector = new GroupNameAttrDetector(); // todo remove instantition

  @Inject
  private GroupObjectClassDetector groupObjectClassDetector = new GroupObjectClassDetector(); // todo remove instantition

  private GroupMemberAttrDetector groupMemberAttrDetector = new GroupMemberAttrDetector(); // todo remove instantition

  @Inject
  public DefaultAttributeDetectionService() {
  }

  @Override
  public AmbariLdapConfiguration detectLdapUserAttributes(LdapConnection connection, AmbariLdapConfiguration ambariLdapConfiguration) {
    LOGGER.info("Detecting LDAP user attributes ...");

    // perform a search using the user search base
    if (Strings.isEmpty(ambariLdapConfiguration.userSearchBase())) {
      LOGGER.warn("No user search base provided");
      return ambariLdapConfiguration;
    }

    SearchCursor searchCursor = null;

    try {
      // todo should the bind operation be done in the facade?
      connection.bind(ambariLdapConfiguration.bindDn(), ambariLdapConfiguration.bindPassword());

      SearchRequest searchRequest = assembleUserSearchRequest(ambariLdapConfiguration);

      // do the search
      searchCursor = connection.search(searchRequest);

      int processedUserCnt = 0;

      while (searchCursor.next()) {

        if (processedUserCnt >= SAMPLE_RESULT_SIZE) {
          LOGGER.debug("The maximum count of results for attribute detection has exceeded. Quit user attribute detection.");
          break;
        }

        Response response = searchCursor.get();
        // process the SearchResultEntry

        if (response instanceof SearchResultEntry) {
          Entry resultEntry = ((SearchResultEntry) response).getEntry();
          LOGGER.info("Processing sample entry with dn: [{}]", resultEntry.getDn());

          userNameAttrDetector.collect(resultEntry);
          userObjectClassDetector.collect(resultEntry);
          userGroupMemberAttrDetector.collect(resultEntry);

          processedUserCnt++;
        }
      }

      ambariLdapConfiguration.setValueFor(AmbariLdapConfiguration.AmbariLdapConfig.USER_NAME_ATTRIBUTE, userNameAttrDetector.detect());
      ambariLdapConfiguration.setValueFor(AmbariLdapConfiguration.AmbariLdapConfig.USER_OBJECT_CLASS, userObjectClassDetector.detect());
      ambariLdapConfiguration.setValueFor(AmbariLdapConfiguration.AmbariLdapConfig.USER_GROUP_MEMBER_ATTRIBUTE, userGroupMemberAttrDetector.detect());

      LOGGER.info("Decorated ambari ldap config : [{}]", ambariLdapConfiguration);

    } catch (Exception e) {

      LOGGER.error("Ldap operation failed", e);
    } finally {
      // housekeeping
      if (null != searchCursor) {
        searchCursor.close();
      }
    }

    return ambariLdapConfiguration;
  }


  @Override
  public AmbariLdapConfiguration detectLdapGroupAttributes(LdapConnection connection, AmbariLdapConfiguration ambariLdapConfiguration) {
    LOGGER.info("Detecting LDAP group attributes ...");

    // perform a search using the user search base
    if (Strings.isEmpty(ambariLdapConfiguration.groupSearchBase())) {
      LOGGER.warn("No group search base provided");
      return ambariLdapConfiguration;
    }

    SearchCursor searchCursor = null;

    try {
      // todo should the bind operation be done in the facade?
      connection.bind(ambariLdapConfiguration.bindDn(), ambariLdapConfiguration.bindPassword());

      SearchRequest searchRequest = assembleGroupSearchRequest(ambariLdapConfiguration);

      // do the search
      searchCursor = connection.search(searchRequest);

      int processedGroupCnt = 0;

      while (searchCursor.next()) {

        if (processedGroupCnt >= SAMPLE_RESULT_SIZE) {
          LOGGER.debug("The maximum number of results for attribute detection has exceeded. Quit  detection.");
          break;
        }

        Response response = searchCursor.get();
        // process the SearchResultEntry

        if (response instanceof SearchResultEntry) {
          Entry resultEntry = ((SearchResultEntry) response).getEntry();
          LOGGER.info("Processing sample entry with dn: [{}]", resultEntry.getDn());

          groupNameAttrDetector.collect(resultEntry);
          groupObjectClassDetector.collect(resultEntry);
          groupMemberAttrDetector.collect(resultEntry);

          processedGroupCnt++;
        }
      }

      ambariLdapConfiguration.setValueFor(AmbariLdapConfiguration.AmbariLdapConfig.GROUP_NAME_ATTRIBUTE, groupNameAttrDetector.detect());
      ambariLdapConfiguration.setValueFor(AmbariLdapConfiguration.AmbariLdapConfig.GROUP_OBJECT_CLASS, groupObjectClassDetector.detect());
      ambariLdapConfiguration.setValueFor(AmbariLdapConfiguration.AmbariLdapConfig.GROUP_MEMBER_ATTRIBUTE, groupMemberAttrDetector.detect());

      LOGGER.info("Decorated ambari ldap config : [{}]", ambariLdapConfiguration);

    } catch (Exception e) {

      LOGGER.error("Ldap operation failed", e);
    } finally {
      // housekeeping
      if (null != searchCursor) {
        searchCursor.close();
      }
    }

    return ambariLdapConfiguration;
  }

  private SearchRequest assembleUserSearchRequest(AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException {
    try {

      SearchRequest req = new SearchRequestImpl();
      req.setScope(SearchScope.SUBTREE);
      req.addAttributes("*");
      req.setTimeLimit(0);
      req.setBase(new Dn(ambariLdapConfiguration.userSearchBase()));
      // the filter must be set!
      req.setFilter(FilterBuilder.present(ambariLdapConfiguration.dnAttribute()).toString());

      return req;

    } catch (Exception e) {
      LOGGER.error("Could not assemble ldap search request", e);
      throw new AmbariLdapException(e);
    }
  }

  private SearchRequest assembleGroupSearchRequest(AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException {
    try {

      SearchRequest req = new SearchRequestImpl();
      req.setScope(SearchScope.SUBTREE);
      req.addAttributes("*");
      req.setTimeLimit(0);
      req.setBase(new Dn(ambariLdapConfiguration.groupSearchBase()));
      // the filter must be set!
      req.setFilter(FilterBuilder.present(ambariLdapConfiguration.dnAttribute()).toString());

      return req;

    } catch (Exception e) {
      LOGGER.error("Could not assemble ldap search request", e);
      throw new AmbariLdapException(e);
    }
  }


}
