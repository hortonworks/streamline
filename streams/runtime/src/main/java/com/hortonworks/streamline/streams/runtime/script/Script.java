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

public abstract class Script<I, O, E> implements Serializable {
    protected final String expression;
    protected final E scriptEngine;

    /**
     * Sets the {@link Expression} to be evaluated by the {@link ScriptEngine}
     */
    public Script(String expression, ScriptEngine<E> scriptEngine) {
        this.expression = expression;
        this.scriptEngine = scriptEngine.getEngine();
    }

    public abstract O evaluate(I input) throws ScriptException;

    @Override
    public String toString() {
        return "Script{" + expression + ", scriptEngine=" + scriptEngine + '}';
    }
}
