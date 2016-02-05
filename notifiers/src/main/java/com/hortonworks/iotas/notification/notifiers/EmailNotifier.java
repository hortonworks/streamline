package com.hortonworks.iotas.notification.notifiers;

import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.common.NotificationContext;
import com.hortonworks.iotas.notification.common.Notifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.event.TransportEvent;
import javax.mail.event.TransportListener;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Notifier for sending email notifications. This uses the JavaMail api to send emails.
 *
 * @see <a href="https://java.net/projects/javamail/pages/Home">JavaMail API</a>
 */
public class EmailNotifier implements Notifier, TransportListener {
    private static final Logger LOG = LoggerFactory.getLogger(EmailNotifier.class);

    /**
     * A wrapper class to hold a key and its default value
     */
    private static class Field {
        Field(String key, String defaultVal) {
            this.key = key;
            this.defaultVal = defaultVal;
        }
        String key;
        String defaultVal;
    }

    private static Field Field(String key, String defaultVal) {
        return new Field(key, defaultVal);
    }

    // configuration properties
    private static final Field PROP_USERNAME = Field("username", "");
    private static final Field PROP_PASSWORD = Field("password", "");
    private static final Field PROP_HOST = Field("host", "localhost");
    private static final Field PROP_PORT = Field("port", "25");
    private static final Field PROP_SSL = Field("ssl", "false");
    private static final Field PROP_STARTTLS = Field("starttls", "false");
    private static final Field PROP_DEBUG = Field("debug", "false");
    private static final Field PROP_PROTOCOL = Field("protocol", "smtp");
    private static final Field PROP_AUTH = Field("auth", "true");

    // SMTP keys
    private static final String SMTP_HOST = "mail.smtp.host";
    private static final String SMTP_PORT = "mail.smtp.port";
    private static final String SMTP_AUTH = "mail.smtp.auth";
    private static final String SMTP_SSL_ENABLE = "mail.smtp.ssl.enable";
    private static final String SMTP_STARTTLS_ENABLE = "mail.smtp.starttls.enable";
    private static final String MAIL_TRANSPORT_PROTOCOL = "mail.transport.protocol";


    // Message fields
    private static final Field FIELD_FROM = Field("from", "admin@localhost");
    private static final Field FIELD_TO = Field("to", "");
    private static final Field FIELD_SUBJECT = Field("subject", "Alert");
    private static final Field FIELD_CONTENT_TYPE = Field("contentType", "text/plain");
    private static final Field FIELD_BODY = Field("body", "Got an alert");
    private static final Field[] MSG_FIELDS = {FIELD_FROM, FIELD_TO, FIELD_SUBJECT, FIELD_CONTENT_TYPE, FIELD_BODY};

    private NotificationContext ctx;
    private Map<Message, String> msgNotificationMap = new ConcurrentHashMap<>();
    private Map<String, String> msgFields;
    private Session emailSession;
    private Transport emailTransport;

    @Override
    public void open(NotificationContext ctx) {
        LOG.debug("EmailNotifier open called with context {}", ctx);
        this.ctx = ctx;
        Map<String, Object> defaultFieldValues = new HashMap<>();
        defaultFieldValues.putAll(ctx.getConfig().getDefaultFieldValues());
        this.msgFields = getMsgFields(defaultFieldValues, null);
        this.emailSession = getEmailSession(ctx.getConfig().getProperties());
        this.emailTransport = getEmailTransport(emailSession, this);
    }

    /**
     * {@inheritDoc} This acks the notification by invoking {@link NotificationContext#ack(String)}
     */
    public void messageDelivered(TransportEvent event) {
        LOG.debug("Got messageDelivered event {}", event);
        String notificationId = msgNotificationMap.remove(event.getMessage());
        if (notificationId != null) {
            ctx.ack(notificationId);
        }
    }

    /**
     * {@inheritDoc} This fails the notification by invoking {@link NotificationContext#fail(String)}
     */
    public void messageNotDelivered(TransportEvent event) {
        LOG.debug("Got messageNotDelivered event {}", event);
        handleFail(event);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * TODO: See if partially delivered should be handled separately
     */
    public void messagePartiallyDelivered(TransportEvent event) {
        LOG.debug("Got messagePartiallyDelivered event {}", event);
        handleFail(event);
    }

    @Override
    public void notify(Notification notification) {
        // merge fieldsAndValues with msgFields
        Map<String, String> fieldsToSend = getMsgFields(notification.getFieldsAndValues(), this.msgFields);

        // validate fieldsToSend
        for (Field field : MSG_FIELDS) {
            String val = fieldsToSend.get(field.key);
            if (val == null || val.isEmpty()) {
                throw new NotifierRuntimeException("Field '" + field.key + "' is empty");
            }
        }
        String notificationId = notification.getId();
        if (notificationId == null) {
            throw new NotifierRuntimeException("Id is null for notification " + notification);
        }
        try {
            Message emailMessage = getEmailMessage(fieldsToSend);
            msgNotificationMap.put(emailMessage, notificationId);
            emailTransport.sendMessage(emailMessage, emailMessage.getAllRecipients());
        } catch (MessagingException ex) {
            LOG.error("Got exception", ex);
        }
    }

    @Override
    public void close() {
        try {
            emailTransport.close();
        } catch (MessagingException ex) {
            LOG.error("Error trying to close email transport", ex);
        }
    }

    @Override
    public boolean isPull() {
        return false;
    }

    @Override
    public List<String> getFields() {
        List<String> fieldNames = new ArrayList<>();
        for (Field field: MSG_FIELDS) {
            fieldNames.add(field.key);
        }
        return fieldNames;
    }

    @Override
    public NotificationContext getContext() {
        return ctx;
    }

    /**
     * Returns a new map containing the values for email message fields from the first map,
     * using values from second map as defaults.
     */
    private Map<String, String> getMsgFields(Map<String, Object> values, Map<String, String> defaults) {
        Map<String, String> fields = new HashMap<>(MSG_FIELDS.length);
        for (Field field : MSG_FIELDS) {
            String val = (String) values.get(field.key);
            fields.put(field.key, val != null ? val : (defaults != null ? defaults.get(field.key): field.defaultVal));
        }
        return fields;
    }


    private String getProperty(Properties properties, Field field) {
        return properties.getProperty(field.key, field.defaultVal);
    }
    /**
     * Return a {@link Session} object initialized with the values
     * from the passed in properties.
     */
    private Session getEmailSession(Properties properties) {
        Properties sessionProps = new Properties();
        sessionProps.put(SMTP_HOST, getProperty(properties, PROP_HOST));
        sessionProps.put(SMTP_PORT, getProperty(properties, PROP_PORT));
        sessionProps.put(SMTP_SSL_ENABLE, getProperty(properties, PROP_SSL));
        sessionProps.put(SMTP_STARTTLS_ENABLE, getProperty(properties, PROP_STARTTLS));
        sessionProps.put(MAIL_TRANSPORT_PROTOCOL, getProperty(properties, PROP_PROTOCOL));
        // init authenticator
        final String userName = getProperty(properties, PROP_USERNAME);
        final String password = getProperty(properties, PROP_PASSWORD);
        Authenticator authenticator = null;
        if (!userName.isEmpty() && !password.isEmpty()) {
            sessionProps.put(SMTP_AUTH, getProperty(properties, PROP_AUTH));
            authenticator = new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(userName, password);
                }
            };
        }
        boolean debug = Boolean.parseBoolean(getProperty(properties, PROP_DEBUG));
        // create session
        LOG.debug("Creating session with properties {}, debug {}", sessionProps, debug);
        Session session = Session.getInstance(sessionProps, authenticator);
        session.setDebug(debug);
        return session;
    }

    /**
     * Return a {@link Transport} object from the session registering the passed in transport listener
     * for delivery notifications.
     */
    private Transport getEmailTransport(Session session, TransportListener listener) {
        try {
            Transport transport = session.getTransport();
            transport.addTransportListener(listener);
            transport.connect();
            LOG.debug("Email transport {}, transport listener {}", transport, listener);
            return transport;
        } catch (MessagingException ex) {
            LOG.error("Got exception while initializing transport", ex);
            throw new NotifierRuntimeException("Got exception while initializing transport", ex);
        }
    }

    /**
     * Construct a {@link Message} from the map of message field values
     */
    private Message getEmailMessage(Map<String, String> fields) throws MessagingException {
        Message msg = new MimeMessage(emailSession);
        msg.setFrom(new InternetAddress(fields.get(FIELD_FROM.key)));
        InternetAddress[] address = {new InternetAddress(fields.get(FIELD_TO.key))};
        msg.setRecipients(Message.RecipientType.TO, address);
        msg.setSubject(fields.get(FIELD_SUBJECT.key));
        msg.setSentDate(new Date());
        Multipart content = new MimeMultipart();
        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(fields.get(FIELD_BODY.key), fields.get(FIELD_CONTENT_TYPE.key));
        content.addBodyPart(mimeBodyPart);
        msg.setContent(content);
        return msg;
    }

    private void handleFail(TransportEvent event) {
        String notificationId = msgNotificationMap.remove(event.getMessage());
        if (notificationId != null) {
            ctx.fail(notificationId);
        }
    }

}
