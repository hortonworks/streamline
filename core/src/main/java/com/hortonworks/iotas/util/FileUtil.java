package com.hortonworks.iotas.util;

import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class FileUtil {

    /**
     * Utility method: write input stream to temporary file
     *
     * @param inputStream input stream
     * @return File object which points temporary file being made
     * @throws IOException
     */
    public static File writeInputStreamToTempFile(InputStream inputStream, String fileNameSuffix)
            throws IOException {
        OutputStream outputStream = null;
        try {
            File tmpFile = File.createTempFile(UUID.randomUUID().toString(), fileNameSuffix);
            tmpFile.deleteOnExit();
            outputStream = new FileOutputStream(tmpFile);
            ByteStreams.copy(inputStream, outputStream);
            return tmpFile;
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException ex) {
                // swallow
            }
        }
    }

}
