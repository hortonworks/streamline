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
package com.hortonworks.streamline.streams.runtime.storm;

import com.hortonworks.streamline.streams.StreamlineEvent;

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
