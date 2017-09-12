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

import java.util.Map;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

public class UserNameAttributeDetector extends OccurranceAndWeightBasedDetector {
  private static final Logger LOGGER = LoggerFactory.getLogger(UserNameAttributeDetector.class);

  private Map<String, Integer> occurranceMap = Maps.newHashMap();
  private Map<String, Integer> weightsMap = Maps.newHashMap();

  private enum NameAttrs {
    SAM_ACCOUNT_NAME("sAMAccountName", 5),
    UID("uid", 3),
    CN("cn", 1);

    private String attrName;
    private Integer weight;

    NameAttrs(String attr, Integer weght) {
      this.attrName = attr;
      this.weight = weght;
    }

    Integer weight() {
      return this.weight;
    }

    String attrName() {
      return this.attrName;
    }

  }

  public UserNameAttributeDetector() {
    for (NameAttrs nameAttr : NameAttrs.values()) {
      occurranceMap.put(nameAttr.attrName(), 0);
      weightsMap.put(nameAttr.attrName(), nameAttr.weight());
    }
  }

  protected Map<String, Integer> occurranceMap() {
    return occurranceMap;
  }

  protected Map<String, Integer> weightsMap() {
    return weightsMap;
  }

  @Override
  protected boolean applies(Entry entry, String value) {
    LOGGER.info("Checking for attribute  [{}] in entry [{}]", value, entry.getDn());
    return entry.containsAttribute(value);
  }

}
