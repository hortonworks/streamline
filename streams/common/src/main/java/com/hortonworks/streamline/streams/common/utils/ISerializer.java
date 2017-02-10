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

package com.hortonworks.streamline.streams.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * The ISerializer interface describes the methods that an object should
 * implement to provide serialization and de-serialization capabilities to
 * non-JVM language components.
 */
public interface ISerializer extends Serializable {

    /**
     * This method sets the input and output streams of the serializer
     *
     * @param processIn output stream to non-JVM component
     * @param processOut input stream from non-JVM component
     */
    void initialize(OutputStream processIn, InputStream processOut);

    /**
     * This method transmits the config to the non-JVM process and
     * receives its pid.
     *
     * @param conf configuration
     * @param context Shell context
     * @param outputStreams  outputStreams
     * @return process pid
     */
    Long connect(Map<String, Object> conf, ShellContext context, List<String> outputStreams) throws IOException,
            NoOutputException;

    /**
     * This method receives a shell message from the non-JVM process
     *
     * @return shell message
     */
    ShellMsg readShellMsg() throws IOException, NoOutputException;

    /**
     * This method sends a Process message to a non-JVM  process
     *
     * @param msg bolt message
     */
    void writeProcessorMsg(ProcessorMsg msg) throws IOException;
}
