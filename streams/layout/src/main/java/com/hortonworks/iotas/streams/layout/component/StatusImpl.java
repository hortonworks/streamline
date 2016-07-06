package com.hortonworks.iotas.streams.layout.component;

import java.util.HashMap;
import java.util.Map;

public class StatusImpl implements TopologyActions.Status {
    String status = "Unknown"; // default
    Map<String, String> extra = new HashMap<>();

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
