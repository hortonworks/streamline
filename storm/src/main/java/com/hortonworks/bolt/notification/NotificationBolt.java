package com.hortonworks.bolt.notification;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import com.hortonworks.client.RestClient;
import com.hortonworks.iotas.catalog.NotifierInfo;
import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.common.Notifier;
import com.hortonworks.iotas.notification.common.NotifierConfig;
import com.hortonworks.iotas.notification.common.NotifierConfigImpl;
import com.hortonworks.iotas.notification.service.NotificationService;
import com.hortonworks.iotas.notification.service.NotificationServiceImpl;

import java.io.File;
import java.util.Map;
import java.util.Properties;

/**
 * This storm bolt receives tuples from rule engine
 * and uses notification service to send out notifications.
 */
public class NotificationBolt extends BaseRichBolt {
    private static final String CATALOG_ROOT_URL = "catalog.root.url";
    public static final String LOCAL_NOTIFIER_JAR_PATH = "local.notifier.jar.path";

    private static final String IOTAS_NOTIFICATION = "iotas.notification";

    private NotificationService notificationService;
    private Notifier notifier;
    private BoltNotificationContext notificationContext;
    private RestClient restClient;

    private final String notifierName;

    /**
     * <p>
     *     The notifier name that this bolt handles. The notifier name should uniquely identify
     *     a notifier instance that the user configured via dashboard (e.g. email_notifier_1).
     * </p>
     * @param notifierName The notifier name associated with this bolt.
     */
    public NotificationBolt(String notifierName) {
        this.notifierName = notifierName;
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        if (!stormConf.containsKey(CATALOG_ROOT_URL)) {
            throw new IllegalArgumentException("conf must contain " + CATALOG_ROOT_URL);
        }
        notificationService = new NotificationServiceImpl();
        restClient = new RestClient(stormConf.get(CATALOG_ROOT_URL).toString());
        NotifierInfo notifierInfo = restClient.getNotifierInfo(this.notifierName);

        String jarPath = String.format("%s%s%s", stormConf.get(LOCAL_NOTIFIER_JAR_PATH).toString(),
                                       File.separator, notifierInfo.getJarFileName());

        Properties props = new Properties();
        props.putAll(notifierInfo.getProperties());
        NotifierConfig notifierConfig = new NotifierConfigImpl(props, notifierInfo.getFieldValues(),
                                                               notifierInfo.getClassName(), jarPath);

        notificationContext = new BoltNotificationContext(collector, notifierConfig);
        notificationService.register(this.notifierName, notificationContext);
    }

    @Override
    public void execute(Tuple tuple) {
        Notification notification = (Notification) tuple.getValueByField(IOTAS_NOTIFICATION);
        notificationContext.track(notification.getId(), tuple);
        // send to notifier
        notificationService.notify(this.notifierName, notification);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
    }

}
