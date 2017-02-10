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
package com.hortonworks.streamline.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * A common configuration class that can be used
 * to store String key and values as configuration.
 */
public class Config extends AbstractConfig {

    public Config() {
    }

    public Config(Config other) {
        super(other);
    }

    /**
     * Construct a Config object from a {@link Properties} file.
     *
     * @param propertiesFile the properties file name
     */
    public Config(String propertiesFile) throws IOException {
        this(new FileInputStream(propertiesFile));
    }

    /**
     * Construct a Config object from an {@link java.io.InputStream}
     * of {@link Properties}.
     *
     * @param inputStream the input stream
     */
    public Config(InputStream inputStream) throws IOException {
        Properties properties = new Properties();
        properties.load(inputStream);
        fromProperties(properties);
    }

    /**
     * Construct a Config object
     * from {@link Properties}
     *
     * @param properties the properties
     */
    public Config(Properties properties) {
        fromProperties(properties);
    }

    public Config(Map<String, String> properties) {
        super(properties);
    }

    @Override
    public String get(String key) {
        return (String) super.get(key);
    }

    @Override
    public String get(String key, Object defaultValue) {
        return super.get(key, defaultValue).toString();
    }

    @Override
    public int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    @Override
    public int getInt(String key, int defaultValue) {
        return Integer.parseInt(get(key, String.valueOf(defaultValue)));
    }

    @Override
    public long getLong(String key) {
        return Long.parseLong(get(key));
    }

    @Override
    public long getLong(String key, long defaultValue) {
        return Long.parseLong(get(key, String.valueOf(defaultValue)));
    }

    @Override
    public double getDouble(String key) {
        return Double.parseDouble(get(key));
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        return Double.parseDouble(get(key, String.valueOf(defaultValue)));
    }

    @Override
    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, String.valueOf(defaultValue)));
    }

    public void put(String key, int value) {
        put(key, String.valueOf(value));
    }

    public void put(String key, long value) {
        put(key, String.valueOf(value));
    }

    public void put(String key, double value) {
        put(key, String.valueOf(value));
    }

    public void put(String key, boolean value) {
        put(key, String.valueOf(value));
    }

    private void fromProperties(Properties properties) {
        for (String key : properties.stringPropertyNames()) {
            put(key, properties.getProperty(key));
        }
    }

    @Override
    public String toString() {
        return "Config{} " + super.toString();
    }
}
