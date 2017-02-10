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
package com.hortonworks.streamline.streams.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.streams.catalog.processor.CustomProcessorInfo;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.layout.exception.ComponentConfigException;
import mockit.Injectable;
import mockit.Tested;
import mockit.VerificationsInOrder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

// TODO fix this test
@Ignore
public class CustomProcessorUploadHandlerTest {
    private final String uuid = UUID.randomUUID().toString();
    private final String uploadWatchDirectory = System.getProperty("java.io.tmpdir") + File.separator + uuid;
    private final String failedUploadMoveDirectory = uploadWatchDirectory + File.separator + "failure";
    private final String successfulUploadMoveDirectory = uploadWatchDirectory + File.separator + "success";
    private final String notTarFileName = "someFile.txt";
    private final String notTarFilePath = uploadWatchDirectory + File.separator + notTarFileName;
    private final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    private InputStream jarFile;
    private final String resourceDirectoryPrefix = "customprocessorupload/";
    CustomProcessorInfo customProcessorInfo;
    @Injectable private StreamCatalogService catalogService;

    private @Tested
    CustomProcessorUploadHandler customProcessorUploadHandler;

    @Before
    public void setup () throws IOException {
        File f = new File(uploadWatchDirectory);
        f.mkdir();
        f = new File(failedUploadMoveDirectory);
        f.mkdir();
        f = new File(successfulUploadMoveDirectory);
        f.mkdir();
        jarFile = new FileInputStream(new File(classLoader.getResource(resourceDirectoryPrefix + "streamline-core.jar").getFile()));
        byte[] data = new byte[1024];
        ObjectMapper mapper = new ObjectMapper();
        FileInputStream fileInputStream = new FileInputStream(new File(classLoader.getResource(resourceDirectoryPrefix + "info.json")
                .getFile()));
        fileInputStream.read(data);
        fileInputStream.close();
        customProcessorInfo = mapper.readValue(data, CustomProcessorInfo.class);
        this.customProcessorUploadHandler = new CustomProcessorUploadHandler(uploadWatchDirectory, failedUploadMoveDirectory, successfulUploadMoveDirectory,
                catalogService);
    }

    @Test
    public void testNotTarFile () throws IOException {
        File f = new File(notTarFilePath);
        f.createNewFile();
        Path path = Paths.get(notTarFilePath);
        this.customProcessorUploadHandler.created(path);
        f = new File(failedUploadMoveDirectory + File.separator + notTarFileName);
        Assert.assertTrue(f.exists());
    }

    @Test
    public void testFailures () throws IOException {
        final String[] fileNames = {"nocustomprocessorinfo.tar", "nojarfile.tar", "nocustomprocessorimpl.tar"};
        for (String fileName: fileNames) {
            URL url = classLoader.getResource(resourceDirectoryPrefix + fileName);
            String consoleCustomProcessorTarString = url.getFile();
            File consoleCustomProcessorTar = new File(consoleCustomProcessorTarString);
            FileUtils.copyFileToDirectory(consoleCustomProcessorTar, new File(uploadWatchDirectory), false);
            this.customProcessorUploadHandler.created(Paths.get(uploadWatchDirectory).resolve(fileName));
            File f = new File(failedUploadMoveDirectory + File.separator + fileName);
            Assert.assertTrue(f.exists());
        }
    }

    @Test
    public void testSuccessfulUpload () throws IOException, ComponentConfigException {
        String fileName = "consolecustomprocessor.tar";
        URL url  = classLoader.getResource(resourceDirectoryPrefix + fileName);
        String consoleCustomProcessorTarString = url.getFile();
        File consoleCustomProcessorTar = new File(consoleCustomProcessorTarString);
        FileUtils.copyFileToDirectory(consoleCustomProcessorTar, new File(uploadWatchDirectory), false);
        this.customProcessorUploadHandler.created(Paths.get(uploadWatchDirectory).resolve(fileName));
        new VerificationsInOrder() {{
            InputStream jarFileActual;
            catalogService.addCustomProcessorInfoAsBundle(withEqual(customProcessorInfo), jarFileActual = withCapture());
            times = 1;
            Assert.assertTrue(IOUtils.contentEquals(jarFileActual, jarFile));
        }};
    }

    @After
    public void cleanup () throws IOException {
        File f = new File(uploadWatchDirectory);
        FileUtils.deleteDirectory(f);
        if (jarFile != null) {
            jarFile.close();
        }
    }
}
