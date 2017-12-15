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
package com.hortonworks.streamline.streams.runtime.storm.event.sample;

import com.google.common.collect.ImmutableMap;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.event.correlation.EventCorrelationInjector;
import com.hortonworks.streamline.streams.storm.common.StormTopologyUtil;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.storm.metric.FileBasedEventLogger;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class StreamlineEventLogger extends FileBasedEventLogger {

    public static final String MARKER_FOR_STREAMLINE_EVENT = "<STREAMLINE_EVENT>";
    public static final String MARKER_FOR_OTHER_EVENT = "<OTHER_EVENT>";
    public static final String DELIMITER = "!_DELIM_!";

    private FastDateFormat dateFormat = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    protected String buildLogMessage(EventInfo event) {
        String timestampStr = dateFormat.format(event.getTs());

        List<Object> values = event.getValues();
        if (!values.isEmpty()) {
            final Object eventObj = values.get(0);

            if (eventObj instanceof StreamlineEvent) {
                final StreamlineEvent slEvent = (StreamlineEvent) eventObj;

                Set<String> rootIds;
                if (EventCorrelationInjector.containsRootIds(slEvent)) {
                    rootIds = EventCorrelationInjector.getRootIds(slEvent);
                } else {
                    rootIds = Collections.emptySet();
                }

                Set<String> parentIds;
                if (EventCorrelationInjector.containsParentIds(slEvent)) {
                    parentIds = EventCorrelationInjector.getParentIds(slEvent);
                } else {
                    parentIds = Collections.emptySet();
                }

                // Date, Marker, Component Name (Streamline), Event ID, Root IDs, Parent IDs,
                // Event Fields, Header KV, Aux. Fields KV
                // use DELIMITER to let parser understand it more easily
                String format = String.join(DELIMITER, new String[] { "%s","%s","%s","%s","%s","%s","%s","%s","%s"} );

                return String.format(format, timestampStr, MARKER_FOR_STREAMLINE_EVENT,
                        StormTopologyUtil.extractStreamlineComponentName(event.getComponent()),
                        slEvent.getId(), rootIds, parentIds, ImmutableMap.copyOf(slEvent),
                        slEvent.getHeader().toString(), slEvent.getAuxiliaryFieldsAndValues().toString());
            }
        }

        // Date, Marker, Component Name (Storm), task ID, Message ID, Values
        // use comma-separated delimiter since this is not for machine, but for users
        Object messageId = event.getMessageId();
        return String.format("%s,%s,%s,%s,%s,%s", timestampStr, MARKER_FOR_OTHER_EVENT,
                event.getComponent(), String.valueOf(event.getTask()),
                (messageId == null ? "" : messageId.toString()), values);
    }

}
