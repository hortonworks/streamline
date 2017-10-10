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
package com.hortonworks.streamline.streams.catalog.storage;

import com.hortonworks.registries.storage.Storable;
import com.hortonworks.registries.storage.StorableTest;
import com.hortonworks.streamline.streams.catalog.File;

import java.util.ArrayList;

public class CatalogTests {


    public ArrayList<StorableTest> getAllInMemoryTests() {
        return new ArrayList<StorableTest>() {{
            add(new FilesTest());
        }};
    }

    public ArrayList<StorableTest> getAllTests() {
        return new ArrayList<StorableTest>() {{
            add(new FilesTest());
        }};
    }

    public class FilesTest extends StorableTest {
        {
            storableList = new ArrayList<Storable>() {{
                add(createFiles(1l, "file-1"));
                add(createFiles(1l, "file-2"));
                add(createFiles(2l, "file-3"));
                add(createFiles(3l, "file-4"));
            }};
        }

        protected File createFiles(Long id, String name) {
            File file = new File();
            file.setId(id);
            file.setName(name);
            file.setStoredFileName("/tmp/parser.jar");
            file.setVersion(0l);
            file.setTimestamp(System.currentTimeMillis());
            return file;
        }
    }

}
