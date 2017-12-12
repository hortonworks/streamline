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

import java.util.Collections;
import java.util.List;

public class LogSearchCriteria {
    private String appId;
    private List<String> componentNames;
    private List<String> logLevels;
    private String searchString;
    private long from;
    private long to;
    private Integer start;
    private Integer limit;

    public String getAppId() {
        return appId;
    }

    public List<String> getComponentNames() {
        return componentNames;
    }

    public List<String> getLogLevels() {
        return logLevels;
    }

    public String getSearchString() {
        return searchString;
    }

    public long getFrom() {
        return from;
    }

    public long getTo() {
        return to;
    }

    public Integer getStart() {
        return start;
    }

    public Integer getLimit() {
        return limit;
    }

    public LogSearchCriteria(String appId, String componentName, String logLevel, String searchString,
                             long from, long to, Integer start, Integer limit) {
        this.appId = appId;
        this.componentNames = Collections.singletonList(componentName);
        this.logLevels = Collections.singletonList(logLevel);
        this.searchString = searchString;
        this.from = from;
        this.to = to;
        this.start = start;
        this.limit = limit;
    }

    public LogSearchCriteria(String appId, List<String> componentNames, List<String> logLevels, String searchString,
                             long from, long to, Integer start, Integer limit) {
        this.appId = appId;
        this.componentNames = componentNames;
        this.logLevels = logLevels;
        this.searchString = searchString;
        this.from = from;
        this.to = to;
        this.start = start;
        this.limit = limit;
    }

    public static class Builder {
        private String appId;
        private List<String> componentNames;
        private List<String> logLevels;
        private String searchString;
        private long from;
        private long to;
        private Integer start;
        private Integer limit;

        public Builder(String appId, long from, long to) {
            this.appId = appId;
            this.from = from;
            this.to = to;
        }

        public Builder setComponentNames(List<String> componentNames) {
            this.componentNames = componentNames;
            return this;
        }

        public Builder setLogLevels(List<String> logLevels) {
            this.logLevels = logLevels;
            return this;
        }

        public Builder setSearchString(String searchString) {
            this.searchString = searchString;
            return this;
        }

        public Builder setStart(Integer start) {
            this.start = start;
            return this;
        }

        public Builder setLimit(Integer limit) {
            this.limit = limit;
            return this;
        }

        public LogSearchCriteria build() {
            return new LogSearchCriteria(appId, componentNames, logLevels, searchString, from, to, start, limit);
        }
    }
}
