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
package com.hortonworks.streamline.streams.logsearch.container.mapping;

public enum MappedTopologyLogSearchImpl {
    STORM_AMBARI_INFRA("com.hortonworks.streamline.streams.logsearch.storm.ambari.AmbariInfraWithStormLogSearch");

    private final String className;

    MappedTopologyLogSearchImpl(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public static String getName(String streamingEngine, String logSearchService) {
        return streamingEngine + "_" + logSearchService;
    }
}
