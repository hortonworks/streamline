package com.hortonworks.iotas.layout.transform;

import com.hortonworks.iotas.common.IotasEvent;

import java.util.Collections;
import java.util.List;

public class IdentityTransform implements Transform {
    @Override
    public List<IotasEvent> execute(IotasEvent input) {
        return Collections.singletonList(input);
    }
}
