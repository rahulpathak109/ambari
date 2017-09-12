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

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.ambari.server.ldap.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.service.AmbariLdapException;
import org.apache.ambari.server.ldap.service.AttributeDetector;
import org.apache.ambari.server.ldap.service.LdapAttributeDetectionService;
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

import com.google.common.collect.Sets;

@Singleton
public class DefaultAttributeDetectionService implements LdapAttributeDetectionService<LdapConnection> {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAttributeDetectionService.class);
  private static final int SAMPLE_RESULT_SIZE = 50;

  // ordered list of possible username attribute values (the most significant valus should be first)

  private static final Set<String> USER_OBJECT_CLASS_VALUES = Sets.newHashSet("person", "posixAccount");
  private static final Set<String> USER_GROUP_MEMBER_ATTR_VALUES = Sets.newHashSet("memberOf", "ismemberOf");

  @Inject
  private UserNameAttributeDetector userNameAttrDetector = new UserNameAttributeDetector(); // todo remove instantition

  @Inject
  private ObjectClassDetector objectClassDetector = new ObjectClassDetector(); // todo remove instantition

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

      SearchRequest searchRequest = assembleSearchRequest(ambariLdapConfiguration);

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
          LOGGER.info("Processing sample entry: [{}]", resultEntry.getDn());
          userNameAttrDetector.collect(resultEntry);
          objectClassDetector.collect(resultEntry);
          processedUserCnt++;
        }
      }

      ambariLdapConfiguration.setValueFor(AmbariLdapConfiguration.AmbariLdapConfig.USER_NAME_ATTRIBUTE, userNameAttrDetector.detect());
      ambariLdapConfiguration.setValueFor(AmbariLdapConfiguration.AmbariLdapConfig.USER_OBJECT_CLASS, objectClassDetector.detect());

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

  private void detectUserAttributes(Entry resultEntry, AttributeDetector attributeDetector) {

    attributeDetector.collect(resultEntry);

//    Set<String> objectClasses = detectUserObjectClass(resultEntry);
//    if (!objectClasses.isEmpty()) {
//      ambariLdapConfiguration.setValueFor(AmbariLdapConfiguration.AmbariLdapConfig.USER_OBJECT_CLASS, StringUtils.join(objectClasses, ","));
//    }


  }

  @Override
  public AmbariLdapConfiguration detectLdapGroupAttributes(LdapConnection connection, AmbariLdapConfiguration ambariLdapConfiguration) {
    LOGGER.info("Detecting LDAP group attributes ...");
    return null;
  }

  private SearchRequest assembleSearchRequest(AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException {
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


  private Set<String> detectUserObjectClass(Entry entry) {
    LOGGER.info("Detecting user object class. Attributes: {}", entry.getAttributes());
    throw new UnsupportedOperationException("Not yet implemented");
  }

  private String detectGroupNameAttribute(Entry entry) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  private Set<String> detectGroupObjectClass(Entry entry) {
    throw new UnsupportedOperationException("Not yet implemented");
  }


}
