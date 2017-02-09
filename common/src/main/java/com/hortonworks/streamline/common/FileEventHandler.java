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

import java.nio.file.Path;

/**
 * An interface expected to be implemented by any Streamline component that needs to handle file watcher events.
 * Currently supported events are create, delete and modify
 */
public interface FileEventHandler {
    /**
     *
     * @return String representing path of the directory to be watched for file events
     */
    String getDirectoryToWatch ();

    /**
     * Handle the file created
     * @param path Path to the file created
     */
    void created (Path path);

    /**
     * Handle the file modified
     * @param path Path to the file modified
     */
    void modified (Path path);

    /**
     * Handle the file deleted
     * @param path Path to the file deleted
     */
    void deleted (Path path);
}
