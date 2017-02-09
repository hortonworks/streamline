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
package com.hortonworks.streamline.common.util;

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
