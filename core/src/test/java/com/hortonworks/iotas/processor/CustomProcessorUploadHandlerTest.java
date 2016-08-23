package com.hortonworks.iotas.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.streams.catalog.processor.CustomProcessorInfo;
import com.hortonworks.iotas.streams.catalog.service.StreamCatalogService;
import mockit.Injectable;
import mockit.Tested;
import mockit.VerificationsInOrder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class CustomProcessorUploadHandlerTest {
    private final String uuid = UUID.randomUUID().toString();
    private final String uploadWatchDirectory = System.getProperty("java.io.tmpdir") + File.separator + uuid;
    private final String failedUploadMoveDirectory = uploadWatchDirectory + File.separator + "failure";
    private final String successfulUploadMoveDirectory = uploadWatchDirectory + File.separator + "success";
    private final String notTarFileName = "someFile.txt";
    private final String notTarFilePath = uploadWatchDirectory + File.separator + notTarFileName;
    private final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    private InputStream imageFile, jarFile;
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
        imageFile = new FileInputStream(new File(classLoader.getResource(resourceDirectoryPrefix + "image.png").getFile()));
        jarFile = new FileInputStream(new File(classLoader.getResource(resourceDirectoryPrefix + "iotas-core.jar").getFile()));
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
        final String[] fileNames = {"nocustomprocessorinfo.tar", "nojarfile.tar", "nocustomprocessorimpl.tar", "noimagefile.tar"};
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
    public void testSuccessfulUpload () throws IOException {
        String fileName = "consolecustomprocessor.tar";
        URL url  = classLoader.getResource(resourceDirectoryPrefix + fileName);
        String consoleCustomProcessorTarString = url.getFile();
        File consoleCustomProcessorTar = new File(consoleCustomProcessorTarString);
        FileUtils.copyFileToDirectory(consoleCustomProcessorTar, new File(uploadWatchDirectory), false);
        this.customProcessorUploadHandler.created(Paths.get(uploadWatchDirectory).resolve(fileName));
        new VerificationsInOrder() {{
            InputStream jarFileActual, imageFileActual;
            catalogService.addCustomProcessorInfo(withEqual(customProcessorInfo), jarFileActual = withCapture(), imageFileActual = withCapture());
            times = 1;
            Assert.assertTrue(IOUtils.contentEquals(jarFileActual, jarFile));
            Assert.assertTrue(IOUtils.contentEquals(imageFileActual, imageFile));
        }};
    }

    @After
    public void cleanup () throws IOException {
        File f = new File(uploadWatchDirectory);
        FileUtils.deleteDirectory(f);
        if (imageFile != null) {
            imageFile.close();
        }
        if (jarFile != null) {
            jarFile.close();
        }
    }
}
