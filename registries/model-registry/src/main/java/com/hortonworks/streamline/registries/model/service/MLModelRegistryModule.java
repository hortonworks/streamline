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

package com.hortonworks.streamline.registries.model.service;

import com.hortonworks.streamline.common.ModuleRegistration;
import com.hortonworks.registries.common.util.FileStorage;
import com.hortonworks.registries.storage.StorageManager;
import com.hortonworks.registries.storage.StorageManagerAware;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class MLModelRegistryModule implements ModuleRegistration, StorageManagerAware {
    private StorageManager storageManager;

    @Override
    public void init(Map<String, Object> config, FileStorage fileStorage) {
    }

    @Override
    public List<Object> getResources() {
        return Collections.singletonList(
                new MLModelRegistryResource(new MLModelRegistryService(storageManager)));
    }

    @Override
    public void setStorageManager(StorageManager storageManager) {
        this.storageManager = storageManager;
    }
}
