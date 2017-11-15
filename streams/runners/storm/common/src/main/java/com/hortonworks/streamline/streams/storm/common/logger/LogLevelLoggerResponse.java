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

import java.util.Map;

public class LogLevelLoggerResponse {
    @JsonProperty("target_level")
    private String targetLevel;

    @JsonProperty("reset_level")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String resetLevel;

    @JsonProperty("timeout")
    private Integer timeout;

    @JsonProperty("timeout_epoch")
    private Long timeoutEpoch;

    public LogLevelLoggerResponse(String targetLevel, String resetLevel, Integer timeout, Long timeoutEpoch) {
        this.targetLevel = targetLevel;
        this.resetLevel = resetLevel;
        this.timeout = timeout;
        this.timeoutEpoch = timeoutEpoch;
    }

    public LogLevelLoggerResponse(String targetLevel, Integer timeout, Long timeoutEpoch) {
        this.targetLevel = targetLevel;
        this.timeout = timeout;
        this.timeoutEpoch = timeoutEpoch;
    }

    public String getTargetLevel() {
        return targetLevel;
    }

    public String getResetLevel() {
        return resetLevel;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public Long getTimeoutEpoch() {
        return timeoutEpoch;
    }

    public static com.hortonworks.streamline.streams.storm.common.logger.LogLevelLoggerResponse of(Map<String, Object> data) {
        String targetLevel = (String) data.get("target_level");
        String resetLevel = (String) data.get("reset_level");
        Number timeout = (Number) data.get("timeout");
        Number timeoutEpoch = (Number) data.get("timeout_epoch");

        return new com.hortonworks.streamline.streams.storm.common.logger.LogLevelLoggerResponse(targetLevel, resetLevel, timeout.intValue(), timeoutEpoch.longValue());
    }
}
