package com.hortonworks.iotas.util;

import java.io.InputStream;
import java.util.Map;

/**
 * Interface abstracting the upload and download of parser jars for IoTaS.
 * IoTaS will provide a default file system based implementation which can be
 * swapped by another implementation using jarStorageImplementationClass
 * property in the iotas.yaml
 */
public interface JarStorage {
    /**
     * The jar storage can be initialized with a set of key/value pairs.
     *
     * @param config the config specific to implementation
     */
    void init(Map<String, String> config);

    /**
     * @param inputStream stream to read the jar content from
     * @param name        identifier of the jar file to be used later to retrieve
     *                    using downloadJar
     * @return the path where the file was uploaded
     * @throws java.io.IOException
     */
    String uploadJar(InputStream inputStream, String name) throws java.io.IOException;

    /**
     *
     * @param name identifier of the jar file to be downloaded that was first
     *             passed during uploadJar
     * @return InputStream representing the jar file
     * @throws java.io.IOException
     */
    InputStream downloadJar(String name) throws java.io.IOException;
}
