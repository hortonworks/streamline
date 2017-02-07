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
/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.hortonworks.streamline.streams.notification.common;

import com.hortonworks.streamline.streams.notification.NotifierConfig;

import java.util.Map;
import java.util.Properties;

/**
 * The notifier config implementation.
 */
public class NotifierConfigImpl implements NotifierConfig {

    private final Properties properties;
    private final Map<String, String> defaultFieldValues;
    private final String className;
    private final String jarPath;

    public NotifierConfigImpl(Properties notifierProps, Map<String, String> defaultFieldValues,
                              String className, String jarPath) {
        this.properties = notifierProps;
        this.defaultFieldValues = defaultFieldValues;
        this.className = className;
        this.jarPath = jarPath;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public Map<String, String> getDefaultFieldValues() {
        return defaultFieldValues;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getJarPath() {
        return jarPath;
    }

    @Override
    public String toString() {
        return "NotifierConfigImpl{" +
                "properties=" + properties +
                ", defaultFieldValues=" + defaultFieldValues +
                ", className='" + className + '\'' +
                ", jarPath='" + jarPath + '\'' +
                '}';
    }
}
