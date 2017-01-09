package com.hortonworks.streamline.storage.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class Utils {
    public static Map<String, Object> readStreamlineConfig(String configFilePath) throws IOException {
        ObjectMapper objectMapper = new YAMLMapper();
        return objectMapper.readValue(new File(configFilePath), Map.class);
    }
}
