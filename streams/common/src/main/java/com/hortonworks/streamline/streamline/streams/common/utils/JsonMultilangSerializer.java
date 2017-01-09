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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;


public class JsonMultilangSerializer implements ISerializer  {
    //ANY CHANGE TO THIS CODE MUST BE SERIALIZABLE COMPATIBLE OR THERE WILL BE PROBLEMS
    private static final long serialVersionUID = 2548814660410474022L;
    private static final Logger LOG = LoggerFactory.getLogger(JsonMultilangSerializer.class);

    public static final String DEFAULT_CHARSET = "UTF-8";

    private transient BufferedWriter processIn;
    private transient BufferedReader processOut;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void initialize(OutputStream processIn, InputStream processOut) {
        try {
            this.processIn = new BufferedWriter(new OutputStreamWriter(processIn, DEFAULT_CHARSET));
            this.processOut = new BufferedReader(new InputStreamReader(processOut, DEFAULT_CHARSET));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Long connect(Map<String, Object> conf, ShellContext context, List<String> outputStreams)
            throws IOException, NoOutputException {
        ConnectMsg connectMsg = new ConnectMsg();
        connectMsg.setPidDir(context.getPidDir());
        connectMsg.setConf(conf);
        connectMsg.setOutputStreams(outputStreams);
        connectMsg.setContext(context);
        writeConnectMsg(connectMsg);

        JsonNode node = (JsonNode) readMessage();
        JsonNode pidNode = node.get("pid");
        Long pid = pidNode.asLong();
        return pid;
    }

    private void writeConnectMsg(ConnectMsg connectMsg) throws IOException {
        String jsonString = objectMapper.writeValueAsString(connectMsg);
        writeString(jsonString);
    }

    @Override
    public void writeProcessorMsg(ProcessorMsg processorMsg) throws IOException {
        String jsonString = objectMapper.writeValueAsString(processorMsg);
        writeString(jsonString);
    }

    private void writeString(String str) throws IOException {
        processIn.write(str);
        processIn.write("\nend\n");
        processIn.flush();
    }

    @Override
    public ShellMsg readShellMsg() throws IOException, NoOutputException {
        String jsonString = readString();

        try {
            ShellMsg shellMsg = objectMapper.readValue(jsonString, ShellMsg.class);
            return shellMsg;
        } catch (IOException e) {
            LOG.error("Error during deserialization of output schema JSON string: {}", jsonString, e);
            throw new RuntimeException(e);
        }

    }

    private Object readMessage() throws IOException, NoOutputException {
        String jsonString = readString();
        Object msg = objectMapper.readValue(jsonString, JsonNode.class);
        if (msg != null) {
            return msg;
        } else {
            throw new IOException("unable to parse: " + jsonString);
        }
    }

    private String readString() throws IOException, NoOutputException {
        StringBuilder line = new StringBuilder();

        while (true) {
            String subline = processOut.readLine();
            if (subline == null) {
                StringBuilder errorMessage = new StringBuilder();
                errorMessage.append("Pipe to subprocess seems to be broken!");
                if (line.length() == 0) {
                    errorMessage.append(" No output read.\n");
                } else {
                    errorMessage.append(" Currently read output: "
                            + line.toString() + "\n");
                }
                throw new NoOutputException(errorMessage.toString());
            }
            if (subline.equals("end")) {
                break;
            }
            if (line.length() != 0) {
                line.append("\n");
            }
            line.append(subline);
        }
        return line.toString();
    }

    private class ConnectMsg {
        String pidDir;
        Map<String, Object> conf;
        ShellContext context;
        List<String> outputStreams;

        public String getPidDir() {
            return pidDir;
        }

        public void setPidDir(String pidDir) {
            this.pidDir = pidDir;
        }

        public Map<String, Object> getConf() {
            return conf;
        }

        public void setConf(Map<String, Object> conf) {
            this.conf = conf;
        }

        public ShellContext getContext() {
            return context;
        }

        public void setContext(ShellContext context) {
            this.context = context;
        }

        public List<String> getOutputStreams() {
            return outputStreams;
        }

        public void setOutputStreams(List<String> outputStreams) {
            this.outputStreams = outputStreams;
        }
    }
}
