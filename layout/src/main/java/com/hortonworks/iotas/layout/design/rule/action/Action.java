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

package com.hortonworks.iotas.layout.design.rule.action;

import com.hortonworks.iotas.common.Schema.Field;
import com.hortonworks.iotas.layout.design.component.Component;

import java.io.Serializable;
import java.util.List;

/**
 * // TODO - OUTDATED JAVADOC
 * Action that has as part of its responsibilities to emit the output (i.e. Schema - tuple for a Storm deployment)
 * that is necessary for the next component, already declared in th the layout, (e.g. HDFS sink, or another processor)
 * to be able to do its job.
 *
 * All the sinks and components associated with this action will be evaluated with the output set by this action. The output set
 * in here becomes the input of the next Sink or Component.
 * @param <F> {@link F}
 **/

/** Action that is at the end of the chain of execution. Once this action is complete, this rule will not be evaluated anymore.
 *  The actions performed by this rule will not interact directly with any other components of the rule system, e.g., other rules,
 *  components, sinks, ...
 **/
public class Action implements Serializable {
    private List<Component> components;  // Can be sinks or processors
    private List<Field> declaredOutput;

    public Action() { }

    /**
     * All downstream components must receive the same input, as defined by getDeclaredOutput.
     * Actions that intend to declare different outputs must be declared in a different rule
     * @return List of downstream components or sinks called as part this action execution
     */
    public List<Component> getComponents() {
        return components;
    }

    public void setComponents(List<Component> components) {
        this.components = components;
    }

    public void setDeclaredOutput(List<Field> declaredOutput) {
        this.declaredOutput = declaredOutput;
    }

    public List<Field> getDeclaredOutput() {
        return declaredOutput;
    }

    @Override
    public String toString() {
        return "Action{" +
                "components=" + components +
                ", declaredOutput=" + declaredOutput +
                '}';
    }
}
