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
package com.hortonworks.streamline.streams.storm.common.logger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LogLevelLoggerRequest {
    @JsonProperty("target_level")
    private String targetLevel;

    @JsonProperty("reset_level")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String resetLevel;

    @JsonProperty("timeout")
    private int timeout;

    public LogLevelLoggerRequest(String targetLevel, int timeout) {
        this.targetLevel = targetLevel;
        this.timeout = timeout;
    }

    public LogLevelLoggerRequest(String targetLevel, String resetLevel, int timeout) {
        this.targetLevel = targetLevel;
        this.resetLevel = resetLevel;
        this.timeout = timeout;
    }

    public String getTargetLevel() {
        return targetLevel;
    }

    public String getResetLevel() {
        return resetLevel;
    }

    public int getTimeout() {
        return timeout;
    }
}
