package org.apache.streamline.streams.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.streamline.storage.catalog.AbstractStorable;
import org.apache.streamline.streams.layout.component.rule.Rule;

import java.io.IOException;

public abstract class BaseRuleInfo extends AbstractStorable {
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
