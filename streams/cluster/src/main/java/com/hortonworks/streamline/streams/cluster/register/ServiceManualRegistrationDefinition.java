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
package com.hortonworks.streamline.streams.cluster.register;

import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

public enum ServiceManualRegistrationDefinition {
    STORM(new String[]{"STORM_UI_SERVER", "NIMBUS"}, new String[]{"storm.yaml"}),
    KAFKA(new String[]{"KAFKA_BROKER"}, new String[]{"server.properties"}),
    ZOOKEEPER(new String[]{"ZOOKEEPER_SERVER"}, new String[]{"zoo.cfg"}),
    HDFS(new String[]{}, new String[]{"core-site.xml","hdfs-site.xml"}),
    HBASE(new String[]{}, new String[]{"hbase-site.xml"}),
    HIVE(new String[]{}, new String[]{"hive-site.xml"});

    private final List<String> requiredComponents;
    private final List<String> requiredConfigFiles;

    ServiceManualRegistrationDefinition(String[] requiredComponents, String[] requiredConfigFiles) {
        this.requiredComponents = convertToList(requiredComponents);
        this.requiredConfigFiles = convertToList(requiredConfigFiles);
    }

    public List<String> getRequiredComponents() {
        return requiredComponents;
    }

    public List<String> getRequiredConfigFiles() {
        return requiredConfigFiles;
    }

    private List<String> convertToList(String[] strings) {
        return Arrays.stream(strings).collect(toList());
    }
}
