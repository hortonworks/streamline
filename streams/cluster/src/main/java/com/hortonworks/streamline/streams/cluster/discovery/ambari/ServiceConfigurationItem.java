/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/
package com.hortonworks.streamline.streams.cluster.discovery.ambari;

/**
 * Data structure of "Service Configuration" in Ambari REST API response.
 */
public class ServiceConfigurationItem {
    private String type;
    private Integer version;
    private String tag;
    private String href;

    public ServiceConfigurationItem(String type, Integer version, String tag, String href) {
      this.type = type;
      this.version = version;
      this.tag = tag;
      this.href = href;
    }

    public String getType() {
      return type;
    }

    public Integer getVersion() {
      return version;
    }

    public String getTag() {
      return tag;
    }

    public String getHref() {
      return href;
    }
  }