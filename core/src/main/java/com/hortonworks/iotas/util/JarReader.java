package com.hortonworks.iotas.util;

import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JarReader {

    /**
     * Extract all class names from Jar input stream
     *
     * @param inputStream input stream which contains Jar
     * @return list of class name
     * @throws IOException
     */
    public static List<String> extractClassNames(InputStream inputStream) throws IOException {
        List<String> classNames = new ArrayList<>();
        ZipInputStream zip = new ZipInputStream(inputStream);
        for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
            if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                // This ZipEntry represents a class. Now, what class does it represent?
                String className = entry.getName().replace('/', '.'); // including ".class"
                classNames.add(className.substring(0, className.length() - ".class".length()));
            }
        }
        return classNames;
    }

    /**
     * Find subtype of classes from file , and return their names
     *
     * @param jarFile File object which points Jar file
     * @param superTypeClass type of class which is
     * @return list of class name
     * @throws IOException
     */
    public static List<String> findSubtypeOfClasses(File jarFile, Class superTypeClass) throws IOException {
        try (InputStream tmpFileInputStream = new FileInputStream(jarFile)) {
            List<String> classNames = JarReader.extractClassNames(tmpFileInputStream);

            URLClassLoader urlClassLoader = new URLClassLoader(
                    new URL[] {new URL("file://" + jarFile.getAbsolutePath())},
                    Thread.currentThread().getContextClassLoader());

            List<String> subTypeClasses = Lists.newArrayList();
            for (String className : classNames) {
                try {
                    Class<?> candidate = Class.forName(className, false, urlClassLoader);
                    if (superTypeClass.isAssignableFrom(candidate)) {
                        subTypeClasses.add(candidate.getCanonicalName());
                    }
                } catch (Throwable ex) {
                    // noop...
                }
            }
            return subTypeClasses;
        }
    }

}
