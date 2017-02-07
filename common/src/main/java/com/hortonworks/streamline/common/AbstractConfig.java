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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * The base class for any type of configuration.
 */
public abstract class AbstractConfig implements Serializable {
    private final Map<String, Object> properties = new HashMap<>();

    public AbstractConfig() {
    }

    public AbstractConfig(AbstractConfig other) {
        properties.putAll(other.properties);
    }

    public AbstractConfig(Map<String, ?> properties) {
        this.properties.putAll(properties);
    }

    public Object get(String key) {
        return getObject(key);
    }

    private Object getObject(String key) {
        Object value = properties.get(key);
        if (value != null) {
            return value;
        } else {
            throw new NoSuchElementException(key);
        }
    }

    public <T> T getAny(String key) {
        return (T) getObject(key);
    }

    public <T> Optional<T> getAnyOptional(String key) {
        return Optional.ofNullable((T) properties.get(key));
    }

    // for unit tests
    public void setAny(String key, Object value) {
        properties.put(key, value);
    }

    public void putAll(Map<String, Object> properties) {
        this.properties.putAll(properties);
    }

    public Object get(String key, Object defaultValue) {
        Object value = properties.get(key);
        return value != null ? value : defaultValue;
    }

    public void put(String key, Object value) {
        properties.put(key, value);
    }

    public String getString(String key) {
        return (String) get(key);
    }

    public String getString(String key, String defaultValue) {
        return (String) get(key, defaultValue);
    }

    public int getInt(String key) {
        return (int) get(key);
    }

    public int getInt(String key, int defaultValue) {
        return (int) get(key, defaultValue);
    }

    public long getLong(String key) {
        return (long) get(key);
    }

    public long getLong(String key, long defaultValue) {
        return (long) get(key, defaultValue);
    }

    public double getDouble(String key) {
        return (double) get(key);
    }

    public double getDouble(String key, double defaultValue) {
        return (double) get(key, defaultValue);
    }

    public boolean getBoolean(String key) {
        return (boolean) get(key);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return (boolean) get(key, defaultValue);
    }

    public boolean contains(String key) {
        return properties.containsKey(key);
    }

    public Map<String, Object> getProperties() {
        return new HashMap<>(properties);
    }

    @Override
    public String toString() {
        return "AbstractConfig{" +
                "properties=" + properties +
                '}';
    }
}
