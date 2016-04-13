/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.hortonworks.iotas.layout.design.splitjoin;

import com.hortonworks.iotas.layout.design.Utils;
import com.hortonworks.iotas.layout.design.component.RulesProcessor;
import com.hortonworks.iotas.layout.design.transform.Transform;

import java.util.Collections;
import java.util.List;

/**
 * Stage has a list of transforms to be applied and send the output to a given stream.
 */
public class StageProcessor extends RulesProcessor {

    public StageProcessor() {
    }

    public StageProcessor(StageAction stageAction) {
        setRules(Collections.singletonList(Utils.createTrueRule(stageAction)));
    }

    @Override
    public String toString() {
        return "StageProcessor{}"+super.toString();
    }
}
