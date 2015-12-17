package com.hortonworks.iotas.topology.storm;

import com.hortonworks.iotas.topology.TopologyLayoutConstants;
import com.hortonworks.iotas.util.exception.BadTopologyLayoutException;

import java.util.List;

public class NotificationBoltFluxComponent extends AbstractFluxComponent {
    @Override
    protected void generateComponent() {
        String boltId = "notificationBolt" + UUID_FOR_COMPONENTS;
        String boltClassName = "com.hortonworks.bolt.notification" +
                ".NotificationBolt";
        String[] constructorArgNames =  {
                TopologyLayoutConstants.JSON_KEY_NOTIFIER_NAME
        };
        List boltConstructorArgs = getConstructorArgsYaml(constructorArgNames);
        String[] configMethodNames = {"withHBaseConfigKey"};
        String[] configKeys = { TopologyLayoutConstants.JSON_KEY_NOTIFIER_CONFIG_KEY };
        List configMethods = getConfigMethodsYaml(configMethodNames,
                configKeys);
        component = createComponent(boltId, boltClassName, null, boltConstructorArgs, configMethods);
        addParallelismToComponent();
    }

    @Override
    public void validateConfig () throws BadTopologyLayoutException {
        super.validateConfig();
        validateStringFields();
    }

    private void validateStringFields () throws BadTopologyLayoutException {
        String[] requiredStringFields = {
            TopologyLayoutConstants.JSON_KEY_NOTIFIER_NAME
        };
        validateStringFields(requiredStringFields, true);
        String[] optionalStringFields = {
            TopologyLayoutConstants.JSON_KEY_NOTIFIER_CONFIG_KEY
        };
        validateStringFields(optionalStringFields, false);
    }
}
