package com.hortonworks.iotas.notification.notifiers.device;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.junit.Test;

public class MQTTDeviceInstanceMockBuilderTest {

    @Test
    public void testMQTTDeviceIdentityMockBuilder() throws Exception {
        MQTTDeviceIdentityMockBuilder mqttDeviceIdentityMockBuilder = new MQTTDeviceIdentityMockBuilder(2, 2);
        mqttDeviceIdentityMockBuilder.build();
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
        String mqttDeviceIdentityMockJson = mapper.writeValueAsString(mqttDeviceIdentityMockBuilder.getDeviceInstance());
        System.out.println(mqttDeviceIdentityMockJson);
    }
}
