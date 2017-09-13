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
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.ambari.server.ldap.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.service.AmbariLdapException;
import org.apache.ambari.server.ldap.service.LdapConfigurationService;
import org.apache.directory.api.ldap.codec.decorators.SearchResultEntryDecorator;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.search.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Implementation of the validation logic using the Apache Directory API.
 */
@Singleton
public class DefaultLdapConfigurationService implements LdapConfigurationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLdapConfigurationService.class);

  @Inject
  public DefaultLdapConfigurationService() {
  }

  @Override
  public void checkConnection(LdapConnection ldapConnection, AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException {

    if (!ldapConnection.isConnected()) {
      LOGGER.error("Could not connect to the LDAP server");
      throw new AmbariLdapException("Could not connect to the LDAP server. Configuration: " + ambariLdapConfiguration);
    }

  }

  /**
   * Checks the user attributes provided in the configuration instance by issuing a search for a (known) test user in the LDAP.
   * Attributes are considered correct if there is at least one entry found.
   *
   * Invalid attributes are signaled by throwing an exception.
   *
   * @param ldapConnection          connection instance used to connect to the LDAP server
   * @param testUserName            the test username
   * @param testPassword            the test password
   * @param ambariLdapConfiguration the available LDAP configuration to be validated
   * @return the DN of the test user
   * @throws AmbariLdapException if an error occurs
   */
  @Override
  public String checkUserAttributes(LdapConnection ldapConnection, String testUserName, String testPassword, AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException {
    String userDn = null;
    EntryCursor entryCursor = null;
    try {
      LOGGER.info("Checking user attributes for user {} r ...", testUserName);

      // set up a filter based on the provided attributes
      String filter = FilterBuilder.and(
        FilterBuilder.equal(SchemaConstants.OBJECT_CLASS_AT, ambariLdapConfiguration.userObjectClass()),
        FilterBuilder.equal(ambariLdapConfiguration.userNameAttribute(), testUserName))
        .toString();

      LOGGER.info("Searching for the user: {} using the search filter: {}", testUserName, filter);
      entryCursor = ldapConnection.search(new Dn(ambariLdapConfiguration.userSearchBase()), filter, SearchScope.SUBTREE);

      // collecting search result entries
      List<Entry> users = Lists.newArrayList();
      for (Entry entry : entryCursor) {
        users.add(entry);
        userDn = entry.getDn().getNormName();
      }

      // there should be at least one user found
      if (users.isEmpty()) {
        String msg = String.format("There are no users found using the filter: [ %s ]. Try changing the attribute values", filter);
        LOGGER.error(msg);
        throw new Exception(msg);
      }

      LOGGER.info("Attibute validation succeeded. Filter: {}", filter);

    } catch (Exception e) {

      LOGGER.error("User attributes validation failed.", e);
      throw new AmbariLdapException(e.getMessage(), e);

    } finally {
      if (null != entryCursor) {
        entryCursor.close();
      }
    }
    return userDn;
  }

  /**
   * Checks whether the provided group related settings are correct.
   * The algorithm implemented in this method per
   *
   * @param ldapConnection          a connecion instance bound to an LDAP server
   * @param userDn                  a user DN to check
   * @param ambariLdapConfiguration the available LDAP configuration to be validated
   * @return
   * @throws AmbariLdapException
   */
  @Override
  public Set<String> checkGroupAttributes(LdapConnection ldapConnection, String userDn, AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException {
    SearchCursor searchCursor = null;
    Set<Response> groupResponses = Sets.newHashSet();

    try {
      LOGGER.info("Checking group attributes for user dn {} ...", userDn);

      // set up a filter based on the provided attributes
      String filter = FilterBuilder.and(
        FilterBuilder.equal(SchemaConstants.OBJECT_CLASS_AT, ambariLdapConfiguration.groupObjectClass()),
        FilterBuilder.equal(ambariLdapConfiguration.groupMemberAttribute(), userDn)
      ).toString();

      LOGGER.info("Searching for the groups the user dn: {} is member of using the search filter: {}", userDn, filter);

      // assemble a search request
      SearchRequest searchRequest = new SearchRequestImpl();
      searchRequest.setFilter(filter);
      searchRequest.setBase(new Dn(ambariLdapConfiguration.groupSearchBase()));
      searchRequest.setScope(SearchScope.SUBTREE);
      // attributes to be returned
      searchRequest.addAttributes(ambariLdapConfiguration.groupMemberAttribute(), ambariLdapConfiguration.groupNameAttribute());

      // perform the search
      searchCursor = ldapConnection.search(searchRequest);

      for (Response response : searchCursor) {
        groupResponses.add(response);
      }

    } catch (Exception e) {

      LOGGER.error("User attributes validation failed.", e);
      throw new AmbariLdapException(e.getMessage(), e);

    } finally {
      if (null != searchCursor) {
        searchCursor.close();
      }
    }

    return processGroupResults(groupResponses, ambariLdapConfiguration);
  }


  /**
   * Extracts meaningful values from the search result.
   *
   * @param groupResponses          the result entries returned by the search
   * @param ambariLdapConfiguration holds the keys of the meaningful attributes
   * @return a set with the group names the test user belongs to
   */
  private Set<String> processGroupResults(Set<Response> groupResponses, AmbariLdapConfiguration ambariLdapConfiguration) {
    Set<String> groupStrSet = Sets.newHashSet();
    for (Response response : groupResponses) {
      Entry entry = ((SearchResultEntryDecorator) response).getEntry();
      groupStrSet.add(entry.get(ambariLdapConfiguration.groupNameAttribute()).get().getString());
    }

    LOGGER.debug("Extracted group names from group search responses: {}", groupStrSet);
    return groupStrSet;
  }


}



