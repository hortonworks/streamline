package com.hortonworks.iotas.notification.notifiers;

import com.hortonworks.iotas.notification.Notification;
import com.hortonworks.iotas.notification.NotificationContext;
import com.hortonworks.iotas.notification.NotifierConfig;
import com.sun.mail.smtp.SMTPTransport;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.event.TransportEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

@RunWith(JMockit.class)
public class EmailNotifierTest {

    EmailNotifier emailNotifier;

    @Mocked
    NotificationContext mockNotificationContext;

    @Mocked
    NotifierConfig mockNotifierConfig;

    @Mocked
    Notification mockNotification;

    @Mocked
    SMTPTransport mockTransport;

    @Mocked
    TransportEvent mockTransportEvent;

    @Before
    public void setUp() throws Exception {
        emailNotifier = new EmailNotifier();
    }

    @Test
    public void testMessageDelivered() throws Exception {

    }

    @Test
    public void testMessageNotDelivered() throws Exception {

    }

    @Test(expected = NotifierRuntimeException.class)
    public void testNotifyWithoutAllFields() throws Exception {
        final Map<String, String> defaultFieldValues = new HashMap() {
            {
                put("from", "foo@bar.com");
            }
        };

        final Map<String, String> fieldsAndValues = new HashMap() {
            {
                put("from", "bar@baz.com");
            }
        };

        new Expectations() {
            {
                mockNotificationContext.getConfig();
                result = mockNotifierConfig;
                mockNotifierConfig.getDefaultFieldValues();
                times = 1;
                result = defaultFieldValues;
                mockNotifierConfig.getProperties();
                result = new Properties();
                mockNotification.getFieldsAndValues();
                times = 1;
                result = fieldsAndValues;
            }
        };
        emailNotifier.open(mockNotificationContext);
        emailNotifier.notify(mockNotification);

    }

    @Test
    public void testNotifyOverride() throws Exception {
        final Map<String, String> defaultFieldValues = new HashMap() {
            {
                put("from", "foo@bar.com");
                put("to", "to@bar.com");
            }
        };

        final Map<String, String> fieldsAndValues = new HashMap() {
            {
                put("from", "bar@baz.com");
            }
        };

        new Expectations() {
            {
                mockNotificationContext.getConfig();
                result = mockNotifierConfig;
                mockNotifierConfig.getDefaultFieldValues();
                times = 1;
                result = defaultFieldValues;
                mockNotifierConfig.getProperties();
                result = new Properties();
                mockNotification.getFieldsAndValues();
                times = 1;
                result = fieldsAndValues;
                mockNotification.getId();
                times = 1;
                result = "ABC";
            }
        };
        emailNotifier.open(mockNotificationContext);
        emailNotifier.notify(mockNotification);

        new Verifications() {
            {
                Message msg = null;
                Address[] addr = null;
                mockTransport.sendMessage(msg = withCapture(), addr = withCapture());
                Assert.assertEquals("bar@baz.com", msg.getFrom()[0].toString());
                Assert.assertEquals("to@bar.com", msg.getAllRecipients()[0].toString());
                //System.out.println(addr);
            }
        };
    }

    private void setupExpectations() {
        final Map<String, String> defaultFieldValues = new HashMap() {
            {
                put("from", "foo@bar.com");
            }
        };

        final Map<String, String> fieldsAndValues = new HashMap() {
            {
                put("to", "to@bar.com");
            }
        };


        new Expectations() {
            {
                mockNotificationContext.getConfig();
                result = mockNotifierConfig;
                mockNotifierConfig.getDefaultFieldValues();
                times = 1;
                result = defaultFieldValues;
                mockNotifierConfig.getProperties();
                result = new Properties();
                mockNotification.getFieldsAndValues();
                times = 1;
                result = fieldsAndValues;
                mockNotification.getId();
                times = 1;
                result = "ABC";
            }
        };

    }

    @Test
    public void testDelivered() throws Exception {
        setupExpectations();
        emailNotifier.open(mockNotificationContext);
        emailNotifier.notify(mockNotification);
        final AtomicReference<Message> msgWrapper = new AtomicReference<>();
        new Verifications() {
            {
                Address[] addr = null;
                Message msg;
                mockTransport.sendMessage(msg = withCapture(), addr = withCapture());
                Assert.assertEquals("foo@bar.com", msg.getFrom()[0].toString());
                Assert.assertEquals("to@bar.com", msg.getAllRecipients()[0].toString());
                msgWrapper.set(msg);
                //System.out.println(addr);
            }
        };

        new Expectations() {
            {
                mockTransportEvent.getMessage(); times = 1;
                result = msgWrapper.get();
            }
        };
        emailNotifier.messageDelivered(mockTransportEvent);
        new Verifications() {
            {
                String id;
                mockNotificationContext.ack(id = withCapture());
                assertEquals("ABC", id);
            }
        };
    }

    @Test
    public void testNotDelivered() throws Exception {
        setupExpectations();
        emailNotifier.open(mockNotificationContext);
        emailNotifier.notify(mockNotification);
        final AtomicReference<Message> msgWrapper = new AtomicReference<>();
        new Verifications() {
            {
                Address[] addr = null;
                Message msg;
                mockTransport.sendMessage(msg = withCapture(), addr = withCapture());
                Assert.assertEquals("foo@bar.com", msg.getFrom()[0].toString());
                Assert.assertEquals("to@bar.com", msg.getAllRecipients()[0].toString());
                msgWrapper.set(msg);
                //System.out.println(addr);
            }
        };

        new Expectations() {
            {
                mockTransportEvent.getMessage(); times = 1;
                result = msgWrapper.get();
            }
        };
        emailNotifier.messageNotDelivered(mockTransportEvent);
        new Verifications() {
            {
                String id;
                mockNotificationContext.fail(id = withCapture());
                assertEquals("ABC", id);
            }
        };
    }

    @Test
    public void testPartiallyDelivered() throws Exception {
        setupExpectations();
        emailNotifier.open(mockNotificationContext);
        emailNotifier.notify(mockNotification);
        final AtomicReference<Message> msgWrapper = new AtomicReference<>();
        new Verifications() {
            {
                Address[] addr = null;
                Message msg;
                mockTransport.sendMessage(msg = withCapture(), addr = withCapture());
                Assert.assertEquals("foo@bar.com", msg.getFrom()[0].toString());
                Assert.assertEquals("to@bar.com", msg.getAllRecipients()[0].toString());
                msgWrapper.set(msg);
                //System.out.println(addr);
            }
        };

        new Expectations() {
            {
                mockTransportEvent.getMessage(); times = 1;
                result = msgWrapper.get();
            }
        };
        emailNotifier.messagePartiallyDelivered(mockTransportEvent);
        new Verifications() {
            {
                String id;
                mockNotificationContext.fail(id = withCapture());
                assertEquals("ABC", id);
            }
        };
    }
}