package com.hortonworks.iotas.notification.notifiers.device;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.junit.Test;


public class DeviceRegistryProcessorMockBuilderTest {

    @Test
    public void testDeviceRegistryProcessorMockBuilder() throws Exception {
        DeviceRegistryProcessor mockDeviceRegistryProcessor = new DeviceRegistryProcessorMockBuilder(1, 1, 1, 1).build();
//        //JSON
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, true);
//        String mockDeviceRegistryProcessorJson = mapper.writeValueAsString(mockDeviceRegistryProcessor);
//        System.out.println(mockDeviceRegistryProcessorJson);
        System.out.println(mockDeviceRegistryProcessor);
    }
}
