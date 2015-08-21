/*
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
package com.hortonworks.iotas.parser;


import com.fasterxml.jackson.core.JsonParseException;
import com.hortonworks.iotas.common.Schema;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * NEVER MAKE AN ASSUMPTION THAT AN IMPLEMENTATION WILL BE SINGLETON. WE MUCK WITH CLASSLOADERS SO IT IS GUARANTEED THAT ANY IMPLEMENTATION OF THIS CLASS WILL NOT BE A SINGLETON.
 */
public interface Parser {
    /**
     * Returns the device version this parser is associated with. This often
     * corresponds to a particular firmware version.
     * @return
     */
    String version();

    /**
     * Returns the format/schema of the data this parser produces.
     * @return
     */
    Schema schema();

    /**
     * Given a byte array, returns a set of key-value
     * pairs conforming to the parser's Schema.
     * @param data
     * @return
     */
    Map<String, Object> parse(byte[] data) throws ParseException, IOException;

    //TODO: don't understand the need for any of the following methods
    /**
     * Parse method that accepts the data in String format.
     * @param data
     * @return
     */
    Map<String, Object> parse(String data) throws ParseException;

    /**
     * <p>Given a byte array, returns a List of values conforming to the parser's Schema. </p>
     * <p>This can be used to receive just the field data.</p>
     * @param data the raw data bytes
     * @return the List of data conforming to {@link Schema#getFields()}
     */
    List<Object> parseFields(byte[] data) throws ParseException;


    /**
     * ParseFields method that accepts the data in String format.
     * @param data
     * @return
     */
    List<Object> parseFields(String data) throws ParseException;

}
