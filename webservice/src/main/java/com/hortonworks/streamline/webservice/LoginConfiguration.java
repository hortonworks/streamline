package com.hortonworks.streamline.webservice;

import java.util.Map;

/**
 * A class representing information for login mechanism for authentication
 */
public class LoginConfiguration {
    private String className;
    private Map<String, String> params;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return "LoginConfiguration{" +
                "className='" + className + '\'' +
                ", params=" + params +
                '}';
    }


}

