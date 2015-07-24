package com.hortonworks.iotas.webservice;

import io.dropwizard.lifecycle.Managed;
import kafka.javaapi.producer.Producer;
import kafka.producer.ProducerConfig;

import java.util.Properties;

public class KafkaProducerManager implements Managed {
    private Producer producer;

    public KafkaProducerManager(IotasConfiguration config){
        Properties props = new Properties();
        props.put("metadata.broker.list", config.getBrokerList());
        props.put("serializer.class", "kafka.serializer.StringEncoder");
        ProducerConfig conf = new ProducerConfig(props);
        this.producer = new Producer<String, String>(conf);
    }

    public Producer getProducer(){
        return this.producer;
    }


    public void start() throws Exception {
        // nothing to do.
    }

    public void stop() throws Exception {
        this.producer.close();
    }
}
