/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hortonworks.iotas.webservice;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

public class IotasConfiguration extends Configuration {

    @NotEmpty
    private String brokerList;

    @NotEmpty
    private String zookeeperHost;

    @NotEmpty
    private String dataStreamActionsImpl;

    @NotEmpty
    private String iotasStormJar;

    @NotNull
    private JarStorageConfiguration jarStorageConfiguration;

    @JsonProperty
    public String getBrokerList(){
        return this.brokerList;
    }

    @JsonProperty
    public void setBrokerList(String brokerList){
        this.brokerList = brokerList;
    }

    public String getZookeeperHost() {
        return zookeeperHost;
    }

    public void setZookeeperHost(String zookeeperHost) {
        this.zookeeperHost = zookeeperHost;
    }

    public String getDataStreamActionsImpl () {
        return dataStreamActionsImpl;
    }

    public void setDataStreamActionsImpl (String dataStreamActionsImpl) {
        this.dataStreamActionsImpl = dataStreamActionsImpl;
    }

    public String getIotasStormJar () {
        return iotasStormJar;
    }

    public void setIotasStormJar (String iotasStormJar) {
        this.iotasStormJar = iotasStormJar;
    }

    @JsonProperty("jarStorageConfiguration")
    public JarStorageConfiguration getJarStorageConfiguration() {
        return this.jarStorageConfiguration;
    }

    @JsonProperty("jarStorageConfiguration")
    public void setJarStorageConfiguration(JarStorageConfiguration configuration) {
        this.jarStorageConfiguration = configuration;
    }
}
