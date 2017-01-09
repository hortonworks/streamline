package org.apache.streamline.streams.runtime.storm;

import org.apache.streamline.streams.StreamlineEvent;

import java.util.Map;

public class StreamlineRuntimeUtil {

    public static Object getFieldValue (StreamlineEvent streamlineEvent, String fieldName) {
        Map fieldValues = streamlineEvent;
        String[] nestedKeys = fieldName.split(StreamlineEvent.NESTED_FIELD_SPLIT_REGEX);
        for (int i = 0; i < (nestedKeys.length - 1); ++i) {
            if (fieldValues == null)
                break;
            fieldValues = (Map) fieldValues.get(nestedKeys[i]);
        }
        return fieldValues != null ? fieldValues.get(nestedKeys[nestedKeys.length - 1]) : null;
    }
}
