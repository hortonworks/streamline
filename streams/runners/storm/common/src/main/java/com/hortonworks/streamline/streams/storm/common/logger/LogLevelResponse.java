package com.hortonworks.streamline.streams.storm.common.logger;

import java.util.HashMap;
import java.util.Map;

public class LogLevelResponse {
    private Map<String, LogLevelLoggerResponse> namedLoggerLevels;

    public LogLevelResponse() {
        this.namedLoggerLevels = new HashMap<>();
    }

    public void addLoggerResponse(String targetPackage, LogLevelLoggerResponse response) {
        namedLoggerLevels.put(targetPackage, response);
    }

    public void addLoggerResponse(String targetPackage, String targetLogLevel, String resetLogLevel,
                                  int timeoutSecs, long timeoutEpoch) {
        namedLoggerLevels.put(targetPackage, new LogLevelLoggerResponse(targetLogLevel, resetLogLevel, timeoutSecs, timeoutEpoch));
    }

    public void addLoggerResponse(String targetPackage, String targetLogLevel, int timeoutSecs, long timeoutEpoch) {
        namedLoggerLevels.put(targetPackage, new LogLevelLoggerResponse(targetLogLevel, timeoutSecs, timeoutEpoch));
    }

    public Map<String, LogLevelLoggerResponse> getNamedLoggerLevels() {
        return namedLoggerLevels;
    }
}
