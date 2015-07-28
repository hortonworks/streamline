package com.hortonworks.iotas.common;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ReflectionHelper {
    private static final Class[] parameters = new Class[]{URL.class};

    public static void addJarToClassPath(String jarFilePath) throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        URL u = (new File(jarFilePath).toURI().toURL());
        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class sysclass = URLClassLoader.class;

        Method method = sysclass.getDeclaredMethod("addURL", parameters);
        boolean isAccesible = method.isAccessible();
        if (!isAccesible) {
            method.setAccessible(true);
        }
        method.invoke(sysloader, new Object[]{u});

        if (!isAccesible) {
            method.setAccessible(isAccesible);
        }

        //TODO: Don't see a scenario where jar is added to classpath but we don't want to load all of its classes.
        loadAllClassesFromJar(jarFilePath);
    }

    /**
     * This will only load .class files, no META-INF no resources.
     * @param jarFilePath
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static void loadAllClassesFromJar(String jarFilePath) throws ClassNotFoundException, IOException {
        JarFile jarFile = new JarFile(jarFilePath);
        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Enumeration e = jarFile.entries();
        while (e.hasMoreElements()) {
            JarEntry je = (JarEntry) e.nextElement();
            if (je.isDirectory() || !je.getName().endsWith(".class")) {
                continue;
            }
            // -6 because of .class
            String className = je.getName().substring(0, je.getName().length() - 6);
            className = className.replace('/', '.');
            Class c = sysloader.loadClass(className);
        }
    }

    public synchronized static boolean isJarInClassPath(String jarFieName) {
        String classpath = System.getProperty("java.class.path");
        //TODO: need to do better than this.
        return classpath.contains(jarFieName);
    }

    public synchronized static boolean isClassLoaded(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static <T> T newInstance(String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return (T) Class.forName(className).newInstance();
    }
}
