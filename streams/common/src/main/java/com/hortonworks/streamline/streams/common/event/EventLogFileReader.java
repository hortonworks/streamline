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
package com.hortonworks.streamline.streams.common.event;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class EventLogFileReader {
    public static final Charset ENCODING_UTF_8 = Charset.forName("UTF-8");

    private final ObjectMapper objectMapper;

    public EventLogFileReader() {
        this.objectMapper = new ObjectMapper();
    }

    public List<EventInformation> loadEventLogFile(File eventLogFile) throws IOException {
        return loadEventLogFileAsStream(eventLogFile).collect(toList());
    }

    public Stream<EventInformation> loadEventLogFileAsStream(File eventLogFile) throws IOException {
        Stream<String> lines = Files.lines(eventLogFile.toPath(), ENCODING_UTF_8);
        return lines.map(line -> {
            try {
                return (EventInformation) objectMapper.readValue(line, new TypeReference<EventInformation>() {});
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public String readFileAsString(File eventLogFile) throws IOException {
        return FileUtils.readFileToString(eventLogFile, ENCODING_UTF_8);
    }
}
