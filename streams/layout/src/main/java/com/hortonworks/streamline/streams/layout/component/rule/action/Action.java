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

package com.hortonworks.streamline.streams.layout.component.rule.action;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Action that is at the end of the chain of execution. Once this action is complete, this rule will not be evaluated anymore.
 * The actions performed by this rule will not interact directly with any other components of the rule system, e.g., other rules,
 * components, sinks, ...
 *
 */
@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="__type")
public abstract class Action implements Serializable {
    protected String name = "default";

    /**
     * Each {@link Action} can have its own output streams to which events should be sent to.
     */
    protected final Set<String> outputStreams = new HashSet<>();

    public Action() {
    }

    public Action(Action other) {
        setName(other.getName());
        setOutputStreams(new HashSet<>(other.getOutputStreams()));
    }

    public abstract Action copy();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getOutputStreams() {
        return Collections.unmodifiableSet(outputStreams);
    }

    public void setOutputStreams(Set<String> outputStreams) {
        this.outputStreams.clear();
        this.outputStreams.addAll(outputStreams);
    }

    @Override
    public String toString() {
        return "Action{" +
                "name='" + name + '\'' +
                ", outputStreams=" + outputStreams +
                '}';
    }
}
