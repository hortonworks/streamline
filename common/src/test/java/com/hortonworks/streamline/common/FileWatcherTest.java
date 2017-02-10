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

import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.VerificationsInOrder;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RunWith(JMockit.class)
public class FileWatcherTest {
    private final String testDirectory = System.getProperty("user.dir");
    private final Path directoryPath = Paths.get(testDirectory);
    private final Path testFilePath = directoryPath.resolve(UUID.randomUUID().toString());
    private @Tested FileWatcher fileWatcher;

    private @Injectable
    FileEventHandler fileEventHandler;

    @Before
    public void setup () {
        List<FileEventHandler> fileEventHandlers = new ArrayList<>();
        fileEventHandlers.add(fileEventHandler);
        new Expectations() {{
            fileEventHandler.getDirectoryToWatch(); returns(testDirectory);
        }};
        fileWatcher = new FileWatcher(fileEventHandlers);
        fileWatcher.register();
    }

    @Test
    public void test() throws IOException {
        boolean hasMoreEvents;
        File f = testFilePath.toFile();
        f.createNewFile();
        hasMoreEvents = fileWatcher.processEvents();
        Assert.assertEquals(true, hasMoreEvents);
        new VerificationsInOrder() {{
            fileEventHandler.created(withEqual(testFilePath));
            times = 1;
        }};
        f.setLastModified(System.currentTimeMillis());
        hasMoreEvents = fileWatcher.processEvents();
        Assert.assertEquals(true, hasMoreEvents);
        new VerificationsInOrder() {{
            fileEventHandler.modified(withEqual(testFilePath));
            times = 1;
        }};
        f.delete();
        hasMoreEvents = fileWatcher.processEvents();
        Assert.assertEquals(true, hasMoreEvents);
        new VerificationsInOrder() {{
            fileEventHandler.deleted(withEqual(testFilePath));
            times = 1;
        }};
    }
}
