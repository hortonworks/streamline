package com.hortonworks.util;

import com.hortonworks.iotas.util.FileSystemJarStorage;
import com.hortonworks.iotas.util.JarStorage;

public class FileSystemJarStorageTest extends AbstractJarStorageTest {
    @Override
    public JarStorage getJarStorage() {
        return new FileSystemJarStorage();
    }
}
