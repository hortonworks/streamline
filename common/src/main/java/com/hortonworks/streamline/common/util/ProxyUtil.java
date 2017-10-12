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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyUtil<O> {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyUtil.class);
    private static final FilenameFilter JAR_FILENAME_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".jar");
        }
    };

    private final Class<O> interfaceClazz;
    private final ClassLoader parentClassLoader;
    private final ConcurrentHashMap<String, ClassLoader> cachedClassLoaders;

    public ProxyUtil(Class<O> interfaceClazz) {
        this(interfaceClazz, ProxyUtil.class.getClassLoader());
    }

    public ProxyUtil(Class<O> interfaceClazz, ClassLoader parentClassLoader) {
        this.interfaceClazz = interfaceClazz;
        this.parentClassLoader = parentClassLoader;
        this.cachedClassLoaders = new ConcurrentHashMap<>();
    }

    public O loadClassFromLibDirectory(Path libDirectory, String classFqdn) throws MalformedURLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        ClassLoader classLoader;
        if (isClassLoadedFromParent(classFqdn)) {
            classLoader = parentClassLoader;
        } else {
            File file = libDirectory.toFile();
            classLoader = findCachedClassLoader(file.getAbsolutePath());
            if (classLoader == null) {
               classLoader = getJarsAddedClassLoader(file);
            }
        }
        O actualObject = initInstanceFromClassloader(classFqdn, classLoader);
        return createClassLoaderAwareProxyInstance(classLoader, actualObject);
    }

    public O loadClassFromJar(String jarPath, String classFqdn)
            throws ClassNotFoundException, MalformedURLException, InstantiationException, IllegalAccessException {
        ClassLoader classLoader;
        if (isClassLoadedFromParent(classFqdn)) {
            classLoader = parentClassLoader;
        } else {
            classLoader = findCachedClassLoader(jarPath);
            if (classLoader == null) {
                classLoader = getJarAddedClassLoader(jarPath);
            }
        }

        O actualObject = initInstanceFromClassloader(classFqdn, classLoader);
        return createClassLoaderAwareProxyInstance(classLoader, actualObject);
    }

    public static Collection<Class<?>> loadAllClassesFromJar(final File jarFile, final Class<?> superTypeClass) throws IOException {
        List<Class<?>> classes = JarReader.findConcreteSubtypesOfClass(jarFile, superTypeClass);
        final ProxyUtil<?> proxyUtil = new ProxyUtil<>(superTypeClass);
        return Collections2.filter(classes, new Predicate<Class<?>>() {
            @Override
            public boolean apply(@Nullable Class<?> s) {
                try {
                    proxyUtil.loadClassFromJar(jarFile.getAbsolutePath(), s.getName());
                    return true;
                } catch (Throwable ex) {
                    LOG.warn("class {} is concrete subtype of {}, but can't be initialized due to exception:", s, superTypeClass, ex);
                    return false;
                }
            }
        });
    }

    public static Collection<String> canonicalNames(Collection<Class<?>> classes) {
        return Collections2.transform(classes, new Function<Class<?>, String>() {
            @Nullable
            @Override
            public String apply(@Nullable Class<?> input) {
                return input.getCanonicalName();
            }
        });
    }

    private ClassLoader findCachedClassLoader(String jarPath) {
        return cachedClassLoaders.get(jarPath);
    }

    private O createClassLoaderAwareProxyInstance(ClassLoader classLoader, O actualObject) {
        InvocationHandler handler = new ClassLoaderAwareInvocationHandler(classLoader, actualObject);
        return (O) Proxy.newProxyInstance(parentClassLoader, new Class<?>[]{interfaceClazz}, handler);
    }

    private O initInstanceFromClassloader(String classFqdn, ClassLoader classLoader) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException {
        Class parserClass = Class.forName(classFqdn, true, classLoader);
        return (O) parserClass.newInstance();
    }

    private ClassLoader getJarAddedClassLoader(String jarPath) throws MalformedURLException {
        ClassLoader classLoader = new MutableURLClassLoader(new URL[0], parentClassLoader);
        URL u = (new File(jarPath).toURI().toURL());
        ((MutableURLClassLoader)classLoader).addURL(u);
        ClassLoader oldCl = cachedClassLoaders.putIfAbsent(jarPath, classLoader);
        if (oldCl != null) {
            // discard and pick old thing
            classLoader = oldCl;
        }
        return classLoader;
    }

    private ClassLoader getJarsAddedClassLoader(File directory) throws MalformedURLException {
        ClassLoader classLoader = new MutableURLClassLoader(new URL[0], parentClassLoader);
        File[] files;
        if ((directory != null) && ((files = directory.listFiles(JAR_FILENAME_FILTER)) != null)) {
            for (File jar : files) {
                URL u = (jar.toURI().toURL());
                ((MutableURLClassLoader) classLoader).addURL(u);
            }
            ClassLoader oldCl = cachedClassLoaders.putIfAbsent(directory.getAbsolutePath(), classLoader);
            if (oldCl != null) {
                // discard and pick old thing
                classLoader = oldCl;
            }
        } else {
            String errorMessage = "Cannot get a class loader with all jars in directory: " + directory;
            LOG.warn(errorMessage);
            throw new RuntimeException(errorMessage);
        }
        return classLoader;
    }

    private boolean isClassLoadedFromParent(String className) {
        try {
            Class.forName(className, false, parentClassLoader);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
