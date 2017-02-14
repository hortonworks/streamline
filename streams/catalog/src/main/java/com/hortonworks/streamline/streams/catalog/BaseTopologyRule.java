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
package com.hortonworks.streamline.streams.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.storage.catalog.AbstractStorable;
import com.hortonworks.streamline.streams.layout.component.rule.Rule;

import java.io.IOException;

public abstract class BaseTopologyRule extends AbstractStorable {
    @JsonIgnore
    protected abstract String getParsedRuleStr();

    @JsonIgnore
    public final Rule getRule() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Rule rule = mapper.readValue(getParsedRuleStr(), Rule.class);
        if (rule.getId() == null) {
            rule.setId(getId());
        }
        return rule;
    }
}
