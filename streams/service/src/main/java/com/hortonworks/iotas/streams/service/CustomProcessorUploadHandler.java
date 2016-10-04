package com.hortonworks.iotas.streams.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.common.FileEventHandler;
import com.hortonworks.iotas.common.util.FileUtil;
import com.hortonworks.iotas.common.util.ProxyUtil;
import com.hortonworks.iotas.streams.catalog.processor.CustomProcessorInfo;
import com.hortonworks.iotas.streams.catalog.service.StreamCatalogService;
import com.hortonworks.iotas.streams.runtime.CustomProcessorRuntime;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Class implementing uploading of custom processor logic.
 */
public class CustomProcessorUploadHandler implements FileEventHandler {
    private static final Logger LOG = LoggerFactory.getLogger(CustomProcessorUploadHandler.class);
    private final String jsonInfoFile = "info.json";
    private final String watchPath;
    private final String uploadFailPath;
    private final String uploadSuccessPath;
    private final StreamCatalogService catalogService;

    /**
     *
     * @param watchPath Path where custom processor files are expected to be for upload
     * @param uploadFailPath Path where custom processors that failed to upload should be stored
     * @param uploadSuccessPath Path where custom processors that succeeded to upload should be stored
     * @param catalogService CatalogService instance for interacting with underlying storage
     */
    public CustomProcessorUploadHandler (String watchPath, String uploadFailPath, String uploadSuccessPath, StreamCatalogService catalogService) {
        this.watchPath = watchPath;
        this.uploadFailPath = uploadFailPath;
        this.uploadSuccessPath = uploadSuccessPath;
        this.catalogService = catalogService;
    }

    @Override
    public String getDirectoryToWatch () {
        return watchPath;
    }

    @Override
    public void created (Path path) {
        File createdFile = path.toFile();
        LOG.info("Created called with " + createdFile);
        boolean succeeded = false;
        try {
            if (createdFile.getName().endsWith(".tar")) {
                LOG.info("Processing file at " + path);
                CustomProcessorInfo customProcessorInfo = this.getCustomProcessorInfo(createdFile);
                if (customProcessorInfo == null) {
                    LOG.warn("No information found for CustomProcessorRuntime in " + createdFile);
                    return;
                }
                InputStream jarFile = this.getJarFile(customProcessorInfo, createdFile);
                if (jarFile == null) {
                    LOG.warn("No jar file found for CustomProcessorRuntime in " + createdFile);
                    return;
                }
                File tempJarFile = FileUtil.writeInputStreamToTempFile(jarFile, ".jar");
                ProxyUtil<CustomProcessorRuntime> customProcessorProxyUtil = new ProxyUtil<>(CustomProcessorRuntime.class);
                CustomProcessorRuntime customProcessorRuntime = customProcessorProxyUtil.loadClassFromJar(tempJarFile.getAbsolutePath(), customProcessorInfo.getCustomProcessorImpl());
                jarFile.reset();
                this.catalogService.addCustomProcessorInfo(customProcessorInfo, jarFile);
                succeeded = true;
            } else {
                LOG.info("Failing unsupported file that was received: " + path);
            }
        } catch (IOException e) {
            LOG.warn("Exception occured while processing tar file: " + createdFile, e);
        } catch (ClassNotFoundException|InstantiationException|IllegalAccessException e) {
            LOG.warn("Could not load a class from jar file implementing CustomProcessorRuntime interface from " + createdFile, e);
        } finally {
            try {
                if (succeeded) {
                    LOG.info("CustomProcessorRuntime uploaded successfully from  " + createdFile + " Moving file to " + uploadSuccessPath);
                    moveFileToSuccessDirectory(createdFile);
                } else {
                    LOG.warn("CustomProcessorRuntime failed to upload from " + createdFile + " Moving file to " + uploadFailPath);
                    moveFileToFailDirectory(createdFile);
                }
            } catch (IOException e1) {
                LOG.warn("Error moving " + createdFile.getAbsolutePath() + " to " + (succeeded ? uploadSuccessPath : uploadFailPath), e1);
            }
        }
    }

    @Override
    public void deleted (Path path) {
        throw new UnsupportedOperationException("Deleted file watcher event not supported for custom processor file handlers.");
    }

    @Override
    public void modified (Path path) {
        throw new UnsupportedOperationException("Modified file watcher event not supported for custom processor file handlers.");
    }

    private CustomProcessorInfo getCustomProcessorInfo (File tarFile) {
        CustomProcessorInfo customProcessorInfo = null;
        byte[] jsonInfoFileBytes = getFileAsByteArray(tarFile, this.jsonInfoFile);
        if (jsonInfoFileBytes != null && jsonInfoFileBytes.length > 0) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                customProcessorInfo = mapper.readValue(jsonInfoFileBytes, CustomProcessorInfo.class);
            } catch (IOException e) {
                LOG.warn("Error while deserializing custom processor info json.", e);
            }
        } else {
            LOG.warn(jsonInfoFile + " not present in tar file: " + tarFile);
        }
        return customProcessorInfo;
    }

    private InputStream getJarFile (CustomProcessorInfo customProcessorInfo, File tarFile) {
        InputStream is = null;
        byte[] jarFileBytes = getFileAsByteArray(tarFile, customProcessorInfo.getJarFileName());
        if (jarFileBytes != null && jarFileBytes.length > 0) {
            is = new ByteArrayInputStream(jarFileBytes);
        } else {
            LOG.warn(customProcessorInfo.getJarFileName() + " not present in tar file: " + tarFile);
        }
        return is;
    }

    private byte[] getFileAsByteArray (File tarFile, String fileName) {
        BufferedInputStream bis = null;
        TarArchiveInputStream tarArchiveInputStream = null;
        byte[] data = null;
        try {
            LOG.info("Getting file " + fileName + " from " + tarFile);
            bis = new BufferedInputStream(new FileInputStream(tarFile));
            tarArchiveInputStream = new TarArchiveInputStream(bis);
            TarArchiveEntry tarArchiveEntry = tarArchiveInputStream.getNextTarEntry();
            while (tarArchiveEntry != null) {
                if (tarArchiveEntry.getName().equals(fileName)) {
                    long size = tarArchiveEntry.getSize();
                    data = new byte[(int) size];
                    tarArchiveInputStream.read(data, 0, data.length);
                    break;
                }
                tarArchiveEntry = tarArchiveInputStream.getNextTarEntry();
            }
        } catch (IOException e) {
            LOG.warn("Exception occured while getting file: " + fileName + " from " + tarFile, e);
        } finally {
            try {
                if (bis != null) {
                    bis.close();
                }
                if (tarArchiveInputStream != null) {
                    tarArchiveInputStream.close();
                }
            } catch (IOException e) {
                LOG.warn("Error closing input stream for the tar file: " + tarFile, e);
            }
        }
        return data;
    }

    private void moveFileToSuccessDirectory (File fileToMove) throws IOException {
        File uploadSuccessDirectory = new File(uploadSuccessPath);
        moveFileToDirectory(fileToMove, uploadSuccessDirectory);
    }

    private void moveFileToFailDirectory (File fileToMove) throws IOException {
        File uploadFailDirectory = new File(uploadFailPath);
        moveFileToDirectory(fileToMove, uploadFailDirectory);
    }

    private void moveFileToDirectory (File fileToMove, File moveToDirectory) throws IOException {
        FileUtils.moveFileToDirectory(fileToMove, moveToDirectory, false);
    }
}
