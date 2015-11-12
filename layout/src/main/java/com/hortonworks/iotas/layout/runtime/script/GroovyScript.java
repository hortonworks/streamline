package com.hortonworks.iotas.layout.runtime.script;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.layout.runtime.script.engine.ScriptEngine;

import javax.script.ScriptException;
import java.util.Map;

/**
 *
 */
public class GroovyScript<O> extends Script<IotasEvent, O, javax.script.ScriptEngine> {

    public GroovyScript(String script, ScriptEngine<javax.script.ScriptEngine> scriptEngine) {
        super(script, scriptEngine);
        log.debug("Created Groovy Script: {}", super.toString());
    }

    @Override
    public O evaluate(IotasEvent iotasEvent) throws ScriptException {
        log.debug("Evaluating {}" + iotasEvent);
        if (iotasEvent != null) {
            //may need to clear all attributes on scriptEngine which may have been set earlier.
            for (Map.Entry<String, Object> fieldAndValue : iotasEvent.getFieldsAndValues().entrySet()) {
                log.debug("{Putting into engine key = {}, val = {}", fieldAndValue.getKey(), fieldAndValue.getValue());
                scriptEngine.put(fieldAndValue.getKey(), fieldAndValue.getValue());
            }
        }
        // Does it compile script for every invocation or does it cache? It may be expensive to evaluate the
        // script for each call.
        // todo optimize this !!
        // GroovyShell gives API to do cache parsed script, script = shell#parse(); script.run() etc
        final O result = (O) scriptEngine.eval(scriptText);
        log.debug("Expression [{}] evaluated to [{}]", scriptText, result);

        return result;
    }
}
