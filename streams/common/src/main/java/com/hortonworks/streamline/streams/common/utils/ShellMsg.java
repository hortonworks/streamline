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
package com.hortonworks.streamline.streams.common.utils;

import java.util.List;
import java.util.Map;

/**
 * ShellMsg is an object that represents the data sent to a shell component from
 * a process that implements a multi-language protocol. It is the union of all
 * data types that a component can send to Streamline.
 *
 */
public class ShellMsg {

    private String command;
    private String msg;
    String  outputStream;
    ShellEvent streamlineEvent;

    public ShellMsg() {
    }

    public String getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(String outputStream) {
        this.outputStream = outputStream;
    }

    public ShellEvent getStreamlineEvent() {
        return streamlineEvent;
    }

    public void setStreamlineEvent(ShellEvent streamlineEvent) {
        this.streamlineEvent = streamlineEvent;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "ShellMsg{" +
                "command='" + command + '\'' +
                ", msg='" + msg + '\'' +
                ", outputStream='" + outputStream + '\'' +
                ", streamlineEvent=" + streamlineEvent +
                '}';
    }

    public static class ShellEvent {

        private Map<String, Object> fieldsAndValues;
        private Object id;
        private String sourceId;
        private String sourceStream;

        public ShellEvent() {
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

        public Map<String, Object> getFieldsAndValues() {
            return fieldsAndValues;
        }

        public void setFieldsAndValues(Map<String, Object> fieldsAndValues) {
            this.fieldsAndValues = fieldsAndValues;
        }

        public Object getId() {
            return id;
        }

        public void setId(Object id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "ShellEvent{" +
                    "fieldsAndValues=" + fieldsAndValues +
                    ", id=" + id +
                    ", sourceId='" + sourceId + '\'' +
                    ", sourceStream='" + sourceStream + '\'' +
                    '}';
        }
    }

}
