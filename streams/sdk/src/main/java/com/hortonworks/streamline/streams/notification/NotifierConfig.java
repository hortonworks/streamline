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
package com.hortonworks.streamline.streams.notification;

import java.util.Map;
import java.util.Properties;

/**
 * A set of notifier specific properties and
 * field values (defaults plus user configured) for
 * the notifier fields.
 */
public interface NotifierConfig {

    /**
     * The class name of this notifier
     */
    String getClassName();

    /**
     * The path of the jar containing the notifier.
     */
    String getJarPath();

    /**
     * The notifier specific properties.
     * E.g. SMTP server, port for email notifier.
     *
     * @return the notifier properties
     */
    Properties getProperties();

    /**
     * The defaults for some of the notifier fields.
     *
     * @return a map of defaults for some of the notifier fields.
     */
    Map<String, String> getDefaultFieldValues();

}
