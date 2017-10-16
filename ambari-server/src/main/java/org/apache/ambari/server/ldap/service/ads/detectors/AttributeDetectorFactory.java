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

package org.apache.ambari.server.ldap.service.ads.detectors;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.ambari.server.ldap.service.AttributeDetector;

@Singleton
public class AttributeDetectorFactory {

  private static final String USER_ATTRIBUTES_DETECTORS = "UserAttributesDetectors";
  private static final String GROUP_ATTRIBUTES_DETECTORS = "GroupAttributesDetectors";


  @Inject
  @Named(USER_ATTRIBUTES_DETECTORS)
  private Set<AttributeDetector> userAttributeDetectors;

  @Inject
  @Named(GROUP_ATTRIBUTES_DETECTORS)
  Set<AttributeDetector> groupAttributeDetectors;

  public AttributeDetectorFactory() {
  }

  public ChainedAttributeDetector userAttributDetector() {
    return new ChainedAttributeDetector(userAttributeDetectors);
  }

  public ChainedAttributeDetector groupAttributDetector() {
    return new ChainedAttributeDetector(groupAttributeDetectors);
  }


}
