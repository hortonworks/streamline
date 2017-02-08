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
package com.hortonworks.streamline.streams.cluster.register;

import com.hortonworks.streamline.common.Config;
import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.Service;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface ManualServiceRegisterer {

    void init(EnvironmentService environmentService);

    Service register(Cluster cluster, Config config, List<ConfigFileInfo> configFileInfos) throws IOException;

    class ConfigFileInfo {
        private final String fileName;
        private final InputStream fileInputStream;

        public ConfigFileInfo(String fileName, InputStream fileInputStream) {
            this.fileName = fileName;
            this.fileInputStream = fileInputStream;
        }

        public String getFileName() {
            return fileName;
        }

        public InputStream getFileInputStream() {
            return fileInputStream;
        }
    }
}
