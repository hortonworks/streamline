package com.hortonworks.iotas.notification.common;

import com.hortonworks.iotas.notification.Notification;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by aiyer on 9/24/15.
 */
public class NotificationImplTest {

    @Test
    public void testDefaultValues() throws Exception {
        Map<String, Object> keyVals = new HashMap<>();

        NotificationImpl impl = new NotificationImpl.Builder(keyVals).build();
        assertEquals(Notification.Status.NEW, impl.getStatus());
        assertNotNull(impl.getId());
    }
}