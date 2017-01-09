/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.streamline.streams.common.utils;

public class ShellContext {

    public ShellContext() {
    }

    private String codeDir;
    private String pidDir;
    private String componentId;

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getCodeDir() {
        return codeDir;
    }

    public void setCodeDir(String codeDir) {
        this.codeDir = codeDir;
    }

    public String getPidDir() {
        return pidDir;
    }

    public void setPidDir(String pidDir) {
        this.pidDir = pidDir;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ShellContext that = (ShellContext) o;

        if (codeDir != null ? !codeDir.equals(that.codeDir) : that.codeDir != null) return false;
        if (pidDir != null ? !pidDir.equals(that.pidDir) : that.pidDir != null) return false;
        return componentId != null ? componentId.equals(that.componentId) : that.componentId == null;

    }

    @Override
    public int hashCode() {
        int result = codeDir != null ? codeDir.hashCode() : 0;
        result = 31 * result + (pidDir != null ? pidDir.hashCode() : 0);
        result = 31 * result + (componentId != null ? componentId.hashCode() : 0);
        return result;
    }
}
