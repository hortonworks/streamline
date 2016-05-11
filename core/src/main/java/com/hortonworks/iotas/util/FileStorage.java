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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.hortonworks.iotas.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Interface abstracting the upload and download of files (like parser jars, split/join or custom processor jars or any other files) for IoTaS.
 * IoTaS will provide a default file system based implementation which can be swapped by another implementation using fileStorageConfiguration
 * property in the iotas.yaml
 */
public interface FileStorage {

    String DEFAULT_DIR = "/tmp/iotas-files";

    /**
     * The file storage can be initialized with a set of key/value pairs.
     *
     * @param config the config specific to implementation
     */
    void init(Map<String, String> config);

    /**
     * Uploads the content from given {@code InputStream} to the configured storage with the given {@code name } as identifier which can
     * be used later for {@link #downloadFile(String)} or {@link #deleteFile(String)}.
     *
     * @param inputStream stream to read the file content from
     * @param name identifier of the file to be used later to retrieve
     *             using {@link #downloadFile(String)}
     * @throws java.io.IOException  if any IO error occurs
     */
    String uploadFile(InputStream inputStream, String name) throws IOException;

    /**
     * Returns {@link InputStream} of file for the given name.
     *
     * @param name identifier of the file to be downloaded that was first
     *             passed during {@link #uploadFile(InputStream, String)}
     * @return InputStream representing the file
     * @throws java.io.IOException if any IO error occurs
     */
    InputStream downloadFile(String name) throws IOException;

    /**
     * Deletes the stored file for given {@code name}.
     *
     * @param name identifier of the file to be deleted that was
     *             passed during {@link #uploadFile(InputStream, String)}
     * @return {@code true} if the file is deleted, {@code false} if the file could not be deleted
     * @throws IOException if any IO error occurs
     */
    boolean deleteFile(String name) throws IOException;

}
