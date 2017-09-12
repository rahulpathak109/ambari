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

/**
 * Operations for detecting LDAP related settings.
 * Attributes and values are detected based on a sample set of results returned from a search
 */
public interface AttributeDetector<T> {

  /**
   * Collects potential attribute names or values from a set of result entries.
   *
   * @param entry a result entry returned by a search operation
   */
  void collect(T entry);

  /**
   * Implements the decision based on whiche the "best" possible attribute or value is selected.
   *
   * @return the most probable attribute name or value (based on the logic in the implementer)
   */
  String detect();


}
