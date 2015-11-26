/*
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

package com.hortonworks.iotas.layout.design.component;

import com.hortonworks.iotas.common.Schema.Field;

import java.io.Serializable;
import java.util.List;

/**
 *  A {@code Sink} is a {@link Component} as from the implementation standpoint there is no difference between them
 */
public class Component implements Serializable {
    private Long id;
    private String name;
    private String description;
    private List<Field> declaredInput;

    // Internal ids
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // Defined by the user
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Input to this component
    public List<Field> getDeclaredInput() {
        return declaredInput;
    }

    public void setDeclaredInput(List<Field> declaredInput) {
        this.declaredInput = declaredInput;
    }

    @Override
    public String toString() {
        return "Component{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", declaredInput=" + declaredInput +
                '}';
    }
}
