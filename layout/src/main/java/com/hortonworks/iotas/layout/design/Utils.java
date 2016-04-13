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
package com.hortonworks.iotas.layout.design;

import com.hortonworks.iotas.layout.design.rule.Rule;
import com.hortonworks.iotas.layout.design.rule.action.Action;

import java.util.Collections;

/**
 * Utility methods to be used in layout module.
 */
public final class Utils {

    private Utils() {
    }

    /**
     * Returns a rule with no condition(which is equivalent to true) with the given action.
     *
     * @param action Action to be set on the rule
     */
    public static Rule createTrueRule(final Action action) {
        Rule rule = new Rule() {{
            setName(action.getName()+"-rule");
            setId(System.currentTimeMillis());
        }};

        rule.setActions(Collections.singletonList(action));
        return rule;
    }
}
