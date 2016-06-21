package com.hortonworks.iotas.layout.runtime.rule;

import com.hortonworks.iotas.topology.component.rule.Rule;
import com.hortonworks.iotas.topology.component.rule.action.Action;
import com.hortonworks.iotas.layout.runtime.ActionRuntime;
import com.hortonworks.iotas.layout.runtime.TransformAction;
import com.hortonworks.iotas.layout.runtime.transform.AddHeaderTransform;
import com.hortonworks.iotas.layout.runtime.transform.IdentityTransform;
import com.hortonworks.iotas.layout.runtime.transform.MergeTransform;
import com.hortonworks.iotas.layout.runtime.transform.ProjectionTransform;
import com.hortonworks.iotas.layout.runtime.transform.SubstituteTransform;
import com.hortonworks.iotas.layout.runtime.transform.Transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractRuleRuntimeBuilder implements RuleRuntimeBuilder {
    protected List<ActionRuntime> actions;

    @Override
    public void buildActions() {
        List<ActionRuntime> runtimeActions = new ArrayList<>();
        Rule rule = getRule();
        for (Action action : rule.getActions()) {
            String streamId = rule.getOutputStreamNameForAction(action);
            /*
             * Add an TransformAction to perform necessary transformation for notification
             */
            runtimeActions.add(new TransformAction(streamId, getTransforms(action)));
        }
        actions = runtimeActions;
    }


    /**
     * Returns the necessary transforms to perform based on the action.
     */
    private List<Transform> getTransforms(Action action) {
        List<Transform> transforms = new ArrayList<>();
        if (action.getOutputFieldsAndDefaults() != null && !action.getOutputFieldsAndDefaults().isEmpty()) {
            transforms.add(new MergeTransform(action.getOutputFieldsAndDefaults()));
            transforms.add(new SubstituteTransform(action.getOutputFieldsAndDefaults().keySet()));
            transforms.add(new ProjectionTransform(action.getOutputFieldsAndDefaults().keySet()));
        }
        if (action.isIncludeMeta()) {
            Map<String, Object> headers = new HashMap<>();
            headers.put(AddHeaderTransform.HEADER_FIELD_NOTIFIER_NAME, action.getNotifierName());
            headers.put(AddHeaderTransform.HEADER_FIELD_RULE_ID, getRule().getId());
            transforms.add(new AddHeaderTransform(headers));
        }
        // default is to just forward the event
        if(transforms.isEmpty()) {
            transforms.add(new IdentityTransform());
        }
        return transforms;
    }

    protected abstract Rule getRule();
}
