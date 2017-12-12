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
package com.hortonworks.streamline.streams.logsearch;

import com.hortonworks.streamline.common.exception.ConfigException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DefaultTopologyLogSearch implements TopologyLogSearch {
  @Override
  public void init(Map<String, Object> conf) throws ConfigException {
    // no-op
  }

  @Override
  public LogSearchResult search(LogSearchCriteria criteria) {
    // no-op. just returning empty result
    return new LogSearchResult(0L, Collections.emptyList());
  }
}
