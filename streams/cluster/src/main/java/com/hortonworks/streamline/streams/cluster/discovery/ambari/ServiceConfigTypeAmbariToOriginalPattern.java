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

import java.util.Optional;

/**
 * This enum defines how Ambari configuration type is converted to original service configuration type.
 * This enum is needed because Ambari names the config different from origin service, and we would want to respect
 * original service's configuration name.
 */
public enum ServiceConfigTypeAmbariToOriginalPattern {
    // 'storm': storm.yaml
    STORM_SITE("storm-site", "storm"),
    // 'server': server.properties
    KAFKA_BROKER("kafka-broker", "server");

    private final String ambariConfType;
    private final String originalConfType;

    ServiceConfigTypeAmbariToOriginalPattern(String ambariConfType, String originalConfType) {
        this.ambariConfType = ambariConfType;
        this.originalConfType = originalConfType;
    }

    public String ambariConfType() {
        return ambariConfType;
    }

    public String originConfType() {
        return originalConfType;
    }

    public static Optional<ServiceConfigTypeAmbariToOriginalPattern> findByAmbariConfType(String ambariConfType) {
        for (ServiceConfigTypeAmbariToOriginalPattern value : values()) {
            if (value.ambariConfType.equals(ambariConfType)) {
                return Optional.of(value);
            }
        }

        return Optional.empty();
    }

    public static Optional<ServiceConfigTypeAmbariToOriginalPattern> findByOriginalConfType(String originalConfType) {
        for (ServiceConfigTypeAmbariToOriginalPattern value : values()) {
            if (value.originalConfType.equals(originalConfType)) {
                return Optional.of(value);
            }
        }

        return Optional.empty();
    }
}
