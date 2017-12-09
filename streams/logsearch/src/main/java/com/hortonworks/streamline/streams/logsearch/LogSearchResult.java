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
package com.hortonworks.streamline.streams.logsearch;

import java.util.List;

public class LogSearchResult {
    private Long matchedDocs;
    private List<LogDocument> documents;

    public LogSearchResult(Long matchedDocs, List<LogDocument> documents) {
        this.matchedDocs = matchedDocs;
        this.documents = documents;
    }

    public Long getMatchedDocs() {
        return matchedDocs;
    }

    public List<LogDocument> getDocuments() {
        return documents;
    }

    public static class LogDocument {
        private String appId;
        private String componentName;
        private String logLevel;
        private String logMessage;
        private String host;
        private Integer port;

        private long timestamp;

        public LogDocument(String appId, String componentName, String logLevel, String logMessage,
                           String host, Integer port, long timestamp) {
            this.appId = appId;
            this.componentName = componentName;
            this.logLevel = logLevel;
            this.logMessage = logMessage;
            this.host = host;
            this.port = port;
            this.timestamp = timestamp;
        }

        public String getAppId() {
            return appId;
        }

        public String getComponentName() {
            return componentName;
        }

        public String getLogLevel() {
            return logLevel;
        }

        public String getLogMessage() {
            return logMessage;
        }

        public String getHost() {
            return host;
        }

        public Integer getPort() {
            return port;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
