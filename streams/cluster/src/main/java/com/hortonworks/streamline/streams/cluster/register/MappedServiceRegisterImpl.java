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

public enum MappedServiceRegisterImpl {
    ZOOKEEPER("com.hortonworks.streamline.streams.cluster.register.impl.ZookeeperServiceRegisterer"),
    STORM("com.hortonworks.streamline.streams.cluster.register.impl.StormServiceRegisterer"),
    KAFKA("com.hortonworks.streamline.streams.cluster.register.impl.KafkaServiceRegisterer"),
    HDFS("com.hortonworks.streamline.streams.cluster.register.impl.HDFSServiceRegisterer"),
    HBASE("com.hortonworks.streamline.streams.cluster.register.impl.HBaseServiceRegisterer"),
    HIVE("com.hortonworks.streamline.streams.cluster.register.impl.HiveServiceRegisterer");

    private final String className;

    MappedServiceRegisterImpl(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public static boolean contains(String serviceName) {
        try {
            MappedServiceRegisterImpl.valueOf(serviceName);
        } catch (IllegalArgumentException e) {
            return false;
        }

        return true;
    }
}
