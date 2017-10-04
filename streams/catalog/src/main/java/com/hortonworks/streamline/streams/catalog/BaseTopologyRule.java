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
import com.hortonworks.registries.common.Schema;
import com.hortonworks.streamline.storage.Storable;
import com.hortonworks.streamline.storage.catalog.AbstractStorable;
import com.hortonworks.streamline.streams.layout.component.rule.Rule;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class BaseTopologyRule extends AbstractStorable {
    public static final String RECONFIGURE = "reconfigure";

    // if the upstream changed, the component may need reconfiguration
    private Boolean reconfigure = false;


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

    @Override
    public Storable fromMap(Map<String, Object> map) {
        setReconfigure((Boolean) map.get(RECONFIGURE));
        return this;
    }

    @JsonIgnore
    @Override
    public Schema getSchema() {
        return Schema.of(Schema.Field.of(RECONFIGURE, Schema.Type.BOOLEAN));
    }

    public Boolean getReconfigure() {
        return reconfigure;
    }

    public void setReconfigure(Boolean reconfigure) {
        this.reconfigure = reconfigure;
    }

    @JsonIgnore
    public abstract Set<String> getInputStreams();
}
