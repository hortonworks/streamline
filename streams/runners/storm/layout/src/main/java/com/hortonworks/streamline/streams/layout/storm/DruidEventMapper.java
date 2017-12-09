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

package com.hortonworks.streamline.streams.layout.storm;

import com.google.common.collect.ForwardingMap;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventUtils;
import org.apache.storm.druid.bolt.ITupleDruidEventMapper;
import org.apache.storm.tuple.ITuple;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Converts {@link ITuple} to Event
 */
public final class DruidEventMapper implements ITupleDruidEventMapper<Map<String, Object>> {

    public static final String DEFAULT_FIELD_NAME = "event";

    private final String eventFiledName;

    public DruidEventMapper(String eventFiledName) {
        this.eventFiledName =  eventFiledName;
    }

    @Override
    public Map<String, Object> getEvent(ITuple tuple) {

        StreamlineEvent event = (StreamlineEvent) tuple.getValueByField(eventFiledName);
        return new DruidEvent(event.addFieldAndValue(DruidBeamFactoryImpl.PROCESSING_TIME, System.currentTimeMillis()));
    }

    public static class DruidEvent extends ForwardingMap<String, Object> {

        private final StreamlineEvent event;

        public DruidEvent(StreamlineEvent event) {
            this.event = event;
        }

        public static boolean isMap(Object map) {
            return map instanceof Map;
        }

        @Override
        protected Map<String, Object> delegate() {
            return event;
        }

        @Override
        public Object get(@Nullable Object key) {
            return StreamlineEventUtils.getFieldValue(event, (String)key);
        }
    }
}
