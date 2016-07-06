package com.hortonworks.iotas.common.util;

import java.net.URL;
import java.net.URLClassLoader;

public class MutableURLClassLoader extends URLClassLoader {
    public MutableURLClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }
}
