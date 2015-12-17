# IoTaS
Internet of Things at Scale.

##How to run locally
From command line execute the following commands:

`cd $IOTAS-HOME`  
`mvn clean install`  
`cd webservice`  
`mvn package`  
`java -cp target/webservice-0.1-SNAPSHOT.jar com.hortonworks.iotas.webservice.IotasApplication server conf/iotas-dev.yaml`

This should start the webserver on localhost port 8080. If you are running storm on the same host you may get 
`java.net.BindException: Address already in use` in which case you should modify `server` section of iotas.yaml.

##Intellij
`Run -> Edit Configuration -> Application -> IotasApplication` in the `Program argument section` add `server $IOTAS-HOME/webservice/conf/iotas.yaml`

Same config can be used to start debugging.

##Loading webserver with some data
`cd $IOTAS-HOME\bin`  
`./load-device.sh`  

Please see `load-device.sh` which is just bunch of curl commands in case you want to add some other objects to webservice's in memory store.

#Running storm topology
First you need to populate your kafka topic, if you have not done so create your kafka topic by executing    
`kafka-topics.sh --create --topic nest-topic --zookeeper localhost:2181 --replication-factor 1 --partitions 3`  

Then run the device simulator CLI to post some sample `IotasMessage` containing nest data to your kafka topic.  
`cd $IOTAS-HOME`  
`java -cp simulator/target/simulator-0.1-SNAPSHOT.jar com.hortonworks.iotas.simulator.CLI -b localhost:9092 -t nest-topic -f simulator/src/main/resources/nest-iotas-messages`

Sometimes the command fails with following exceptions:
`Exception in thread "main" kafka.common.FailedToSendMessageException: Failed to send messages after 3 tries.
    at kafka.producer.async.DefaultEventHandler.handle(DefaultEventHandler.scala:90)
	at kafka.producer.Producer.send(Producer.scala:76)
	at kafka.javaapi.producer.Producer.send(Producer.scala:33)
	at com.hortonworks.iotas.simulator.CLI.writeToKafka(CLI.java:182)
	at com.hortonworks.iotas.simulator.CLI.processDataFile(CLI.java:109)
	at com.hortonworks.iotas.simulator.CLI.main(CLI.java:74)
`
You should verify from kafka's server.properties that the listerner port and host matches. if advertised host is actual hostname like 'HW10960.local' localhost may not work.

You can run the simulator command in a loop or run it multiple times to produce same data again and again.

##HBase set up

Before starting hbase, put the below hbase-site.xml in the hbase config directory so that hbase uses the local ZK instance running in your localhost and localfs for storage so that you dont need to start a separate HDFS server instance.

```xml
<configuration>
  <property>
    <name>hbase.rootdir</name>
    <value>file:///tmp/hbase</value>
  </property>
  <property>
    <name>hbase.cluster.distributed</name>
    <value>true</value>
  </property>
  <property>
    <name>hbase.zookeeper.quorum</name>
    <value>localhost</value>
  </property>
</configuration>
  ```

Now you need to create HBase tables where the IotasEvent and Notifications will be stored.
  
`hbase shell`

`create 'nest', 'cf', 'd'`

`create 'Notification', 'd', 'f', 's', 'e', 'r', 'nn', 'ts'`

`create 'Timestamp_Notification', 'ni', 'd', 'f', 's', 'e', 'r', 'nn', 'ts'`

`create 'Rule_Notification', 'ni', 'd', 'f', 's', 'e', 'r', 'nn', 'ts'`

`create 'Rule_Status_Notification', 'ni', 'd', 'f', 's', 'e', 'r', 'nn', 'ts'`

`create 'Datasource_Notification', 'ni', 'd', 'f', 's', 'e', 'r', 'nn', 'ts'`

`create 'Datasource_Status_Notification', 'ni', 'd', 'f', 's', 'e', 'r', 'nn', 'ts'`

`create 'Notifier_Notification', 'ni', 'd', 'f', 's', 'e', 'r', 'nn', 'ts'`

`create 'Notifier_Status_Notification', 'ni', 'd', 'f', 's', 'e', 'r', 'nn', 'ts'`


## Running the Topology
Now, From intellij you should be able to run `com.hortonworks.topology.IotasTopology` by providing `$IOTAS-HOME/storm/src/main/resources/topology.yaml` as argument and modifying `$IOTAS-HOME/storm/pom.xml` so `storm-core` is not in provided scope. 
you can also run the topology on a storm cluster by providing the name of the topology as second argument. RIGHT NOW THE TOPOLOGY DOES NOT EXECUTE IN A STORM CLUSTER AS THE JACKSON LIBRARY USED BY US HAVE A CONFLICTING
VERSION WITH STORM. Please merge https://github.com/apache/storm/pull/702 on your local storm cluster if you need to execute the topology on a storm cluster.

`storm jar $IOTAS-HOME/storm/target/storm-0.1-SNAPSHOT.jar com.hortonworks.topology.IotasTopology  $IOTAS-HOME/storm/src/main/resources/topology.yaml IotasTopology`

##IoTaS topology using Flux  
After doing a mvn package on IoTaS home directory you can run the following 
command `storm jar ./storm/target/storm-0.1-SNAPSHOT.jar org.apache.storm.flux.Flux --local --sleep 3600000 --filter ./storm/src/main/resources/flux_iotas_topology.properties ./storm/src/main/resources/flux_iotas_topology_config.yaml`
  
Note that in order to submit the topology created using UI the storm jar for the topology must be copied to the path referred to by iotasStormJar config 
property. Default value for that location is /tmp/storm-0.1-SNAPSHOT.jar

This will run the IOTaS topology in local mode for one hour, processing any events published to the 'nest-topic'. You can kill the topology anytime by pressing CTRL + C in the console.


#Accounting for bad tuples in a topology
A mechanism has been added so that when messages are being played from a 
spout in an IotaS topology and they cant successfully be parsed then such 
messages end up in some persistent storage. The way it works is ParserBolt 
needs to be supplied with two stream ids using builder methods 
withParsedTuplesStreamId and withUnparsedTuplesStreamId. The former stream id
is mandatory and has to be supplied to the bolt. The latter is optional and 
can be used tap the tuples that could not be parsed by the parser. Any 
subsequent component can subscribe to this stream and get the tuples that 
failed to parse using the field `bytes`. In sample topology in storm module 
an HdfsBolt is used to store the unparsed tuples.


#Accessing UI
http://localhost:8080/ui/index.html



