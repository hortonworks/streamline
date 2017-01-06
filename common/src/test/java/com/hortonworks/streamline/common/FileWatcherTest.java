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
