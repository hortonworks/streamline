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

package com.hortonworks.streamline.streams.layout.component.rule.action.transform;

import com.hortonworks.streamline.streams.layout.Transform;

import java.util.Map;

/**
 * Adds a fixed header to the input event
 */
public class AddHeaderTransform extends Transform {
    private final Map<String, Object> fixedHeader;

    private AddHeaderTransform() {
        this(null);
    }

    public AddHeaderTransform(Map<String, Object> fixedHeader) {
        this.fixedHeader = fixedHeader;
    }

    public Map<String, Object> getFixedHeader() {
        return fixedHeader;
    }

    @Override
    public String toString() {
        return "AddHeaderTransform{" +
                "fixedHeader=" + fixedHeader +
                '}'+super.toString();
    }
}
