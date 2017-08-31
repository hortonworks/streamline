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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JarReader {
    private static final Logger LOG = LoggerFactory.getLogger(JarReader.class);

    /**
     * Extract all class names from Jar input stream directory contents.
     * These are fqdnNames, not canonicalNames, inner class names will still
     * have '$' characters in them.
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
     * Find concrete (instantiable) subtypes of a given superType from file,
     * and return their reference Class objects.
     *
     * @param jarFile File object which points Jar file
     * @param superTypeClass type of class desired
     * @return list of classes
     * @throws IOException
     */
    public static List<Class<?>> findConcreteSubtypesOfClass(File jarFile, Class superTypeClass) throws IOException {
        try (InputStream tmpFileInputStream = new FileInputStream(jarFile)) {
            List<String> classNames = JarReader.extractClassNames(tmpFileInputStream);

            URLClassLoader urlClassLoader = new URLClassLoader(
                    new URL[] {new URL("file://" + jarFile.getAbsolutePath())},
                    Thread.currentThread().getContextClassLoader());

            List<Class<?>> subTypeClasses = Lists.newArrayList();
            for (String className : classNames) {
                try {
                    Class<?> candidate = Class.forName(className, false, urlClassLoader);
                    if (superTypeClass.isAssignableFrom(candidate)
                            && isConcrete(candidate)) {
                        subTypeClasses.add(candidate);
                    }
                } catch (Throwable ex) {
                    // noop...
                }
            }
            return subTypeClasses;
        }
    }

    public static boolean isConcrete(Class clazz) {
        return 0 == (clazz.getModifiers() & (Modifier.INTERFACE | Modifier.ABSTRACT));
    }

}
