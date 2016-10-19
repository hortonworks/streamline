package org.apache.streamline.streams.metrics;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for implementations of TimeSeriesQuerier
 */
public abstract class AbstractTimeSeriesQuerier implements TimeSeriesQuerier {
    protected Map<String, String> parseParameters(String parameters) {
        Map<String, String> parsed = new HashMap<>();
        if (parameters != null && !parameters.trim().isEmpty()) {
            String[] splittedParams = parameters.split(",");
            for (String splittedParam : splittedParams) {
                String[] keyValue = splittedParam.split("=");
                if (keyValue.length < 2) {
                    throw new IllegalArgumentException("parameters contain broken key-value pair: " + splittedParam);
                }

                StringBuilder sb = new StringBuilder();
                for (int idx = 1 ; idx < keyValue.length ; idx++) {
                    if (idx != 1) {
                        sb.append('=');
                    }
                    sb.append(keyValue[idx].trim());
                }

                parsed.put(keyValue[0], sb.toString());
            }
        }
        return parsed;
    }
}
