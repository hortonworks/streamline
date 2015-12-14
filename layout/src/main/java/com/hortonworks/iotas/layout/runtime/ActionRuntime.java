package com.hortonworks.iotas.layout.runtime;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.layout.design.rule.Rule;
import com.hortonworks.iotas.layout.transform.IdentityTransform;
import com.hortonworks.iotas.layout.transform.Transform;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Runtime abstraction for the action to be taken when a rule matches the condition
 */
public class ActionRuntime implements Serializable {
    private final String stream;
    private final List<Transform> transforms;

    /**
     * Creates a new {@link ActionRuntime}
     *
     * @param stream the stream where the results are sent out
     */
    public ActionRuntime(String stream) {
        this(stream, Collections.<Transform>singletonList(new IdentityTransform()));
    }

    /**
     * Creates a new {@link ActionRuntime}
     *
     * @param stream  the stream where the results are sent out
     * @param transforms the chain of transformations to be applied (in order)
     */
    public ActionRuntime(String stream, List<Transform> transforms) {
        this.stream = stream;
        this.transforms = transforms;
    }

    public Result execute(IotasEvent input) {
        return new Result(stream, doTransform(input, transforms));
    }

    // applies the transformation chain to the input and returns the transformed events
    private List<IotasEvent> doTransform(IotasEvent input, List<Transform> transforms) {
        return doTransform(input, transforms, 0);
    }

    /**
     * applies the i th transform and recursively invokes the method to apply
     * the rest of the transformations in the chain.
     */
    private List<IotasEvent> doTransform(IotasEvent input, List<Transform> transforms, int i) {
        if (i >= transforms.size()) {
            return Collections.singletonList(input);
        }
        List<IotasEvent> transformed = new ArrayList<>();
        for (IotasEvent event : transforms.get(i).execute(input)) {
            transformed.addAll(doTransform(event, transforms, i + 1));
        }
        return transformed;
    }

    /**
     * Wraps the list of {@link IotasEvent} produced as a result
     * of applying the action and the output stream where this
     * has to be sent.
     */
    public static class Result {
        public final String stream;
        public final List<IotasEvent> events;

        /**
         * Create a new Result
         *
         * @param stream the stream where the result has to be sent
         * @param events the list of events in the result
         */
        public Result(String stream, List<IotasEvent> events) {
            this.stream = stream;
            this.events = events;
        }
    }

    /**
     * The stream where the result of this action are sent out
     *
     * @return the stream where the result of this action are sent out
     */
    public String getStream() {
        return stream;
    }
}
