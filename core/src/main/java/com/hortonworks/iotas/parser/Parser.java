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


import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.exception.DataValidationException;
import com.hortonworks.iotas.exception.ParserException;

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
    Map<String, Object> parse(byte[] data) throws ParserException;

    //TODO: don't understand the need for any of the following methods
    /**
     * Parse method that accepts the data in String format.
     * @param data
     * @return
     */
    Map<String, Object> parse(String data) throws ParserException;

    /**
     * <p>Given a byte array, returns a List of values conforming to the parser's Schema. </p>
     * <p>This can be used to receive just the field data.</p>
     * @param data the raw data bytes
     * @return the List of data conforming to {@link Schema#getFields()}
     */
    List<?> parseFields(byte[] data) throws ParserException;


    /**
     * ParseFields method that accepts the data in String format.
     * @param data
     * @return
     */
    List<?> parseFields(String data) throws ParserException;

    /**
     * Validates an input sequence of bytes and throws {@link DataValidationException} if the data is invalid.
     * The implementation of this method is optional, i.e. it's up to the discretion of the parser
     * implementation to decide if raw data should be validated prior to being parsed.
     * @param rawData the input sequence of bytes to validate
     */
    void validate(byte[] rawData) throws DataValidationException;

    /**
     * Validates the the parsed output of an input sequence and throws {@link DataValidationException} if the data is invalid.
     * Typically the parsed output will be a result of a call to {@link #parse(byte[])} or {@link #parse(String)}
     * The implementation of this method is optional, i.e. it's up to the discretion of the parser
     * implementation to decide if the parsed data should be validated after being parsed.
     * @param parsedData the parsed output of an input sequence to validate
     */
    void validate(Map<String, Object> parsedData) throws DataValidationException;


}
