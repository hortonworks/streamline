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

import com.hortonworks.streamline.streams.StreamlineEvent;
import org.apache.storm.opentsdb.OpenTsdbMetricDatapoint;
import org.apache.storm.opentsdb.bolt.ITupleOpenTsdbDatapointMapper;
import org.apache.storm.tuple.ITuple;

import java.util.Map;

public class OpenTsdbTupleDatapointMapper implements ITupleOpenTsdbDatapointMapper {
    private final String metricField;
    private final String timestampField;
    private final String valueField;
    private final String tagsField;

    public OpenTsdbTupleDatapointMapper(String metricField, String timestampField, String tagsField, String valueField) {
        this.metricField = metricField;
        this.timestampField = timestampField;
        this.tagsField = tagsField;
        this.valueField = valueField;
    }

    @Override
    public OpenTsdbMetricDatapoint getMetricPoint(ITuple tuple) {
        StreamlineEvent event = (StreamlineEvent) tuple.getValueByField(StreamlineEvent.STREAMLINE_EVENT);
        return new OpenTsdbMetricDatapoint(
                (String) event.get(metricField),
                (Map<String, String>) event.get(tagsField),
                (Long) event.get(timestampField),
                (Number) event.get(valueField));
    }

    /**
     * @return metric field name in the tuple.
     */
    public String getMetricField() {
        return metricField;
    }

    /**
     * @return timestamp field name in the tuple.
     */
    public String getTimestampField() {
        return timestampField;
    }

    /**
     * @return value field name in the tuple.
     */
    public String getValueField() {
        return valueField;
    }

    /**
     * @return tags field name in the tuple
     */
    public String getTagsField() {
        return tagsField;
    }

    @Override
    public String toString() {
        return "OpenTsdbTupleDatapointMapper{" +
                "metricField='" + metricField + '\'' +
                ", timestampField='" + timestampField + '\'' +
                ", valueField='" + valueField + '\'' +
                ", tagsField='" + tagsField + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OpenTsdbTupleDatapointMapper)) return false;

        OpenTsdbTupleDatapointMapper that = (OpenTsdbTupleDatapointMapper) o;

        if (getMetricField() != null ? !getMetricField().equals(that.getMetricField()) : that.getMetricField() != null)
            return false;
        if (getTimestampField() != null ? !getTimestampField().equals(that.getTimestampField()) : that.getTimestampField() != null)
            return false;
        if (getValueField() != null ? !getValueField().equals(that.getValueField()) : that.getValueField() != null)
            return false;
        return getTagsField() != null ? getTagsField().equals(that.getTagsField()) : that.getTagsField() == null;
    }

    @Override
    public int hashCode() {
        int result = getMetricField() != null ? getMetricField().hashCode() : 0;
        result = 31 * result + (getTimestampField() != null ? getTimestampField().hashCode() : 0);
        result = 31 * result + (getValueField() != null ? getValueField().hashCode() : 0);
        result = 31 * result + (getTagsField() != null ? getTagsField().hashCode() : 0);
        return result;
    }
}
