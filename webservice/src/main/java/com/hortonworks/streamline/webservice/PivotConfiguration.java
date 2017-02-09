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
package com.hortonworks.streamline.webservice;



import javax.validation.constraints.NotNull;

import java.util.Map;

public class PivotConfiguration {

    @NotNull
    private Integer port;

    private Map<String, Object> config;

    private Map<String, Object> settingsLocation;

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public Map<String, Object> getSettingsLocation() { return settingsLocation; }

    public void setSettingsLocation(Map<String, Object> settingsLocation) { this.settingsLocation = settingsLocation; }
}
