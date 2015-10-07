package com.hortonworks.util;

import com.hortonworks.iotas.util.FileSystemJarStorage;
import com.hortonworks.iotas.util.JarStorage;

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
