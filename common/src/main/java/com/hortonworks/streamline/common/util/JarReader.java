/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/
package com.hortonworks.streamline.common.util;

import com.google.common.collect.Lists;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
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
     * @return list of classes
     * @throws IOException
     */
    public static List<Class<?>> findSubtypeOfClasses(File jarFile, Class superTypeClass) throws IOException {
        try (InputStream tmpFileInputStream = new FileInputStream(jarFile)) {
            List<String> classNames = JarReader.extractClassNames(tmpFileInputStream);

            URLClassLoader urlClassLoader = new URLClassLoader(
                    new URL[] {new URL("file://" + jarFile.getAbsolutePath())},
                    Thread.currentThread().getContextClassLoader());

            List<Class<?>> subTypeClasses = Lists.newArrayList();
            for (String className : classNames) {
                try {
                    Class<?> candidate = Class.forName(className, false, urlClassLoader);
                    if (superTypeClass.isAssignableFrom(candidate)) {
                        subTypeClasses.add(candidate);
                    }
                } catch (Throwable ex) {
                    // noop...
                }
            }
            return subTypeClasses;
        }
    }

}
