package com.hortonworks.iotas.layout.runtime;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.layout.runtime.transform.IdentityTransform;
import com.hortonworks.iotas.layout.runtime.transform.Transform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TransformAction implements ActionRuntime {
    private final String stream;
    private final List<Transform> transforms;

    /**
     * Creates a new {@link TransformAction}
     *
     * @param stream the stream where the results are sent out
     */
    public TransformAction(String stream) {
        this(stream, Collections.<Transform>singletonList(new IdentityTransform()));
    }

    /**
     * Creates a new {@link TransformAction}
     *
     * @param stream  the stream where the results are sent out
     * @param transforms the chain of transformations to be applied (in order)
     */
    public TransformAction(String stream, List<Transform> transforms) {
        this.stream = stream;
        this.transforms = transforms;
    }

    /**
     * {@inheritDoc}
     * Recursively applies the list of {@link Transform} (s) associated with this
     * TransformAction object and returns the {@link Result}
     */
    @Override
    public Result execute(IotasEvent input) {
        return new Result(stream, doTransform(input));
    }

    /*
     * applies the transformation chain to the input and returns the transformed events
     */
    private List<IotasEvent> doTransform(IotasEvent input) {
        return doTransform(input, 0);
    }

    /*
     * applies the i th transform and recursively invokes the method to apply
     * the rest of the transformations in the chain.
     */
    private List<IotasEvent> doTransform(IotasEvent input, int i) {
        if (i >= transforms.size()) {
            return Collections.singletonList(input);
        }
        List<IotasEvent> transformed = new ArrayList<>();
        for (IotasEvent event : transforms.get(i).execute(input)) {
            transformed.addAll(doTransform(event, i + 1));
        }
        return transformed;
    }

    @Override
    public String getStream() {
        return stream;
    }

    @Override
    public String toString() {
        return "TransformAction{" +
                "stream='" + stream + '\'' +
                ", transforms=" + transforms +
                '}';
    }
}
