package com.hortonworks.util;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by pshah on 8/7/15.
 * Interface abstracting the upload and download of parser jars for IoTaS.
 * IoTaS will provide a default file system based implementation which can be
 * swapped by another implementation using jarStorageImplementationClass
 * property in the iotas.yaml
 */
public interface JarStorage {
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
