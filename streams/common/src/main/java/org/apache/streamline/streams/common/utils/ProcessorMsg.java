/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.streamline.streams.common.utils;

import java.util.Map;

/**
 * ProcessorMsg is an object that represents the data sent from a shell component to
 * a processor that implements a multi-language protocol. It is the union of
 * all data types that a processor can receive from StreamLine.
 *
 */
public class ProcessorMsg {
    private String id;
    private String sourceId;
    private String sourceStream;
    private Map<String, Object> fieldsAndValues;

    public Map<String, Object> getFieldsAndValues() {
        return fieldsAndValues;
    }

    public void setFieldsAndValues(Map<String, Object> fieldsAndValues) {
        this.fieldsAndValues = fieldsAndValues;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceStream() {
        return sourceStream;
    }

    public void setSourceStream(String sourceStream) {
        this.sourceStream = sourceStream;
    }

}
