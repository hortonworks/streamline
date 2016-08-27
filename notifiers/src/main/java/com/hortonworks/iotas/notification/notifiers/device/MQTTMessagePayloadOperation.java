package com.hortonworks.iotas.notification.notifiers.device;

import java.util.Map;

public class MQTTMessagePayloadOperation {
    private static final String OPERATION_NAME = "operationName";
    private static final String OPERATION_PARAM = "operationParam";
    private static final String OPERATION_VALUE = "operationValue";

    private Map<String, Object> operationMap;

    //for jackson
    public MQTTMessagePayloadOperation() {
    }

    public Map<String, Object> createOperationMap(Map<String, Object> fieldsFromOperationMap)
    {
        operationMap.put(OPERATION_NAME,fieldsFromOperationMap.get("operationName"));
        operationMap.put(OPERATION_NAME,fieldsFromOperationMap.get("operationParam"));
        operationMap.put(OPERATION_NAME,fieldsFromOperationMap.get("operationValue"));
        return operationMap;
    }
}
