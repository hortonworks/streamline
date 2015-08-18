package com.hortonworks.util;

/**
 * Created by pshah on 8/7/15.
 *
 */
public class FileSystemJarStorageTest extends AbstractJarStorageTest {
    @Override
    public JarStorage getJarStorage() {
        return new FileSystemJarStorage();
    }
}
