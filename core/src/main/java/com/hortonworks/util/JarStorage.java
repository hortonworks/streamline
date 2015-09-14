package com.hortonworks.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Created by pshah on 8/7/15.
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
     *
     * @param inputStream stream to read the jar content from
     * @param name identifier of the jar file to be used later to retrieve
     *             using downloadJar
     * @throws java.io.IOException
     */
    void uploadJar(InputStream inputStream, String name) throws java.io.IOException;

    /**
     *
     * @param name identifier of the jar file to be downloaded that was first
     *             passed during uploadJar
     * @return InputStream representing the jar file
     * @throws java.io.IOException
     */
    InputStream downloadJar(String name) throws java.io.IOException;
}
