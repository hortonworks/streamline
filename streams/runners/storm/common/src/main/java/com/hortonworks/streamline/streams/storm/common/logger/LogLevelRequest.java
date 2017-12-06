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

import java.util.HashMap;
import java.util.Map;

public class LogLevelRequest {
    private Map<String, LogLevelLoggerRequest> namedLoggerLevels;

    public LogLevelRequest() {
        this.namedLoggerLevels = new HashMap<>();
    }

    public void addLoggerRequest(String targetPackage, String targetLogLevel, String resetLogLevel, int timeoutSecs) {
        namedLoggerLevels.put(targetPackage, new LogLevelLoggerRequest(targetLogLevel, resetLogLevel, timeoutSecs));
    }

    public void addLoggerRequest(String targetPackage, String targetLogLevel, int timeoutSecs) {
        namedLoggerLevels.put(targetPackage, new LogLevelLoggerRequest(targetLogLevel, timeoutSecs));
    }

    public Map<String, LogLevelLoggerRequest> getNamedLoggerLevels() {
        return namedLoggerLevels;
    }
}
