Quickstart
===========

In this example, we will ingest data into kafka topic by encoding with SchemaRegistry serdes.

Build Streamline application to consume this data and write one or more rules to ingest into another Kafka topic.

Make sure you run through the deployement phase of docker before this.

Create Kafka Topics
---------------------

    Let's create following topics.

::

       ./streamline-docker.sh shell u-kafka-0
       ./bin/kafka-topics.sh --zookeeper u-zk:2181 --topic truck_events_stream --partitions 1 --replication-factor 1 --create
       ./bin/kafka-topics.sh --zookeeper u-zk:2181 --topic truck_events_output --partitions 1 --replication-factor 1 --create


Here, `truck_events_stream` is our source topic and `truck_events_output` is where we write to.


Create Schemas for the topics
-------------------------------

 To access Schema Regisgtry from your host (osx laptop).

::

      ./streamline-docker.sh port u-schema-registry

paste the port thats pointing to `9090/tcp` in your browser

Click on create schema. For the Avro schema you can find it here  https://raw.githubusercontent.com/harshach/registry/test-branch/examples/schema-registry/avro/src/main/resources/truck_events.avsc

Copy paste the contents like below image for topic `truck_events_stream`

.. figure:: https://raw.githubusercontent.com/harshach/streamline/test-branch/docs/images/registry-add-schema.png

Lets repeat the above step for topic `truck_events_stream`


.. figure:: https://raw.githubusercontent.com/harshach/streamline/test-branch/docs/images/registry-add-schema-output.png


Ingest data into Kafka topic
------------------------------
 Lets log into SchemaRegistry Shell

   ::

      ./streamline-docker.sh shell u-schema-registry
      cd examples/schema-registry/avro/
      vi data/kafka-producer.props


Edit kafka-producer.props like above and change bootstrap.servers property to point

   ::

      bootstrap.servers=u-kafka-0:9092

Rest of the properties can stay the same. Now run the below command to ingest some data


   ::

      java -jar ./avro-examples-0.5.1.jar -sm -s data/truck_events.avsc -d data/truck_events_json -p data/kafka-producer.props 


Above command will ingect 100 messages into the `truck_events_stream` . We can repeat the above command ingest more data.


Build & Deploy Streamline Application
----------------------------------------

To access streamline from your laptop run the below command to figure out the port

::

      ./streamline-docker.sh port u-streamline-0

pick the port pointing to `8080/tcp`

1. Create Service Pool

   1.1 Click on the left-side toolbar spanner icon and choose service pool. Once you are on the page click `Manual` button.

.. figure::  https://raw.githubusercontent.com/harshach/streamline/test-branch/docs/images/streamline-service-pool.png

  Once you fill in the Name and Description for the cluster, click on the + sign to add a service. Lets add the below services


  1.2 Zookeeper

.. figure::  https://raw.githubusercontent.com/harshach/streamline/test-branch/docs/images/zookeeper.png


  1.3 Kafka

.. figure::  https://raw.githubusercontent.com/harshach/streamline/test-branch/docs/images/kafka.png

  1.4 Storm

.. figure:: https://raw.githubusercontent.com/harshach/streamline/test-branch/docs/images/storm.png


Make sure you change the Storm UI default port to 8099. If this is not configured properly we won't be able to deploy the application.


2. Create Environments

Click on the left-side toolbar spanner icon and choose Environements. Click on the icons to select all the services we've configured in ServicePool.

.. figure:: https://raw.githubusercontent.com/harshach/streamline/test-branch/docs/images/environment.png

3. Create an Application

3.1

.. figure:: https://raw.githubusercontent.com/harshach/streamline/test-branch/docs/images/environment.png

   Make sure to pick the environment you just created.



3.2  Drag & Drop the Kafka from Sources

.. figure:: https://raw.githubusercontent.com/harshach/streamline/test-branch/docs/images/environment.png

Pick `truck_events_stream` as the topic and fill in the rest of details such as the `schema version` and `consumer group id`


3.3  Add Rules Processor

.. figure:: https://raw.githubusercontent.com/harshach/streamline/test-branch/docs/images/rule-processor.png

In this we are creating a rule where driverId not equals to zero.

3.4 Drag & drop the Kafka from Sinks

.. figure:: https://raw.githubusercontent.com/harshach/streamline/test-branch/docs/images/kafka-sink.png

Pick `truck_events_output` as the topic.


3.5  Application should look like below

.. figure:: https://raw.githubusercontent.com/harshach/streamline/test-branch/docs/images/topology.png


3.6 Deploy the application

.. figure:: https://raw.githubusercontent.com/harshach/streamline/test-branch/docs/images/deploy.png


3.7 Access Storm UI

Once the application is deployed , we can look at storm UI to get the metrics on how the application is doing.

   ::

       ./streamline-docker.sh port u-storm-ui


.. figure:: https://raw.githubusercontent.com/harshach/streamline/test-branch/docs/images/storm-ui-homepage.png


3.8 Topology metrics
   
.. figure::https://raw.githubusercontent.com/harshach/streamline/test-branch/docs/images/storm-topology.png

The above metrics indicates that our application was able to read data from Kafka topic and run through rule processor and write to another Kafka topic.


3.9 Verify the data

     We can verify if we are able to write to the target kafka topic by using the same tool that we used to ingest data.

     ::

          ./stremaline-docker.sh shell u-schema-registry
          cd  examples/schema-registry/avro/
          vi data/kafka-consumer.props

     and edit the following configs

    ::

          topic=truck_events_output
          bootstrap.servers=u-kafka-0:9092

    Run the consumer like below

    ::


         java -jar avro-examples-0.5.1.jar -cm -c data/kafka-consumer.props

   You should be able to see the ingested messages.
