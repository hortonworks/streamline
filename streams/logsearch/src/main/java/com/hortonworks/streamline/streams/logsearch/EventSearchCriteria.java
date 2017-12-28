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

public class EventSearchCriteria {
    private String appId;
    private List<String> componentNames;
    private String searchString;
    private String searchEventId;
    private long from;
    private long to;
    private Integer start;
    private Integer limit;
    private Boolean ascending;

    public String getAppId() {
        return appId;
    }

    public List<String> getComponentNames() {
        return componentNames;
    }

    public String getSearchString() {
        return searchString;
    }

    public String getSearchEventId() {
        return searchEventId;
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

    public Boolean getAscending() {
        return ascending;
    }

    public EventSearchCriteria(String appId, List<String> componentNames, String searchString,
                               String searchEventId, long from, long to, Integer start, Integer limit,
                               Boolean ascending) {
        this.appId = appId;
        this.componentNames = componentNames;
        this.searchString = searchString;
        this.searchEventId = searchEventId;
        this.from = from;
        this.to = to;
        this.start = start;
        this.limit = limit;
        this.ascending = ascending;
    }

    public static class Builder {
        private String appId;
        private List<String> componentNames;
        private String searchString;
        private String searchEventId;
        private long from;
        private long to;
        private Integer start;
        private Integer limit;
        private Boolean ascending;

        public Builder(String appId, long from, long to) {
            this.appId = appId;
            this.from = from;
            this.to = to;
        }

        public Builder setComponentNames(List<String> componentNames) {
            this.componentNames = componentNames;
            return this;
        }

        public Builder setSearchString(String searchString) {
            this.searchString = searchString;
            return this;
        }

        public Builder setSearchEventId(String searchEventId) {
            this.searchEventId = searchEventId;
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

        public Builder setAscending(Boolean ascending) {
            this.ascending = ascending;
            return this;
        }

        public EventSearchCriteria build() {
            return new EventSearchCriteria(appId, componentNames, searchString, searchEventId,
                    from, to, start, limit, ascending);
        }
    }
}