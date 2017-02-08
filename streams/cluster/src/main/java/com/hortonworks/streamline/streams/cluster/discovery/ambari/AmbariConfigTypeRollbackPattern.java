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

public enum AmbariConfigTypeRollbackPattern {
    // 'storm': storm.yaml
    STORM_SITE("storm-site", "storm"),
    // 'server': server.properties
    KAFKA_BROKER("kafka-broker", "server");

    private final String ambariConfType;
    private final String originConfType;

    AmbariConfigTypeRollbackPattern(String ambariConfType, String originConfType) {
        this.ambariConfType = ambariConfType;
        this.originConfType = originConfType;
    }

    public String ambariConfType() {
        return ambariConfType;
    }

    public String originConfType() {
        return originConfType;
    }

    public static String findOriginConfType(String ambariConfType) {
        AmbariConfigTypeRollbackPattern pattern = lookupByAmbariConfType(ambariConfType);
        if (pattern != null) {
            return pattern.originConfType;
        }

        return null;
    }

    public static AmbariConfigTypeRollbackPattern lookupByAmbariConfType(String ambariConfType) {
        for (AmbariConfigTypeRollbackPattern value : values()) {
            if (value.ambariConfType.equals(ambariConfType)) {
                return value;
            }
        }

        return null;
    }

    public static AmbariConfigTypeRollbackPattern lookupByOriginConfType(String originConfType) {
        for (AmbariConfigTypeRollbackPattern value : values()) {
            if (value.originConfType.equals(originConfType)) {
                return value;
            }
        }

        return null;
    }
}
