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
package com.hortonworks.streamline.streams.udaf;

import com.hortonworks.streamline.streams.rule.UDAF;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects elements within a group and returns the list of aggregated objects
 */
public class CollectList implements UDAF<List<Object>, Object, List<Object>> {
    @Override
    public List<Object> init() {
        return new ArrayList<>();
    }

    @Override
    public List<Object> add(List<Object> aggregate, Object val) {
        aggregate.add(val);
        return aggregate;
    }

    @Override
    public List<Object> result(List<Object> aggregate) {
        return aggregate;
    }
}
