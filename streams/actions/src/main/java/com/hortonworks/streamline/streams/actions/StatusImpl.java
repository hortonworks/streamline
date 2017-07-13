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
package com.hortonworks.streamline.streams.actions;

import java.util.HashMap;
import java.util.Map;

public class StatusImpl implements TopologyActions.Status {
    private String status = STATUS_UNKNOWN; // default
    private final Map<String, String> extra = new HashMap<>();

    public void setStatus(String status) {
        this.status = status;
    }

    public void putExtra(String key, String val) {
        extra.put(key, val);
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public Map<String, String> getExtra() {
        return extra;
    }
}
