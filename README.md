# IoTaS
Internet of Things at Scale.

##How to run locally
From command line execute the following commands:

`cd $IOTAS-HOME`  
`mvn clean install`  
`cd webservice`  
`mvn package`  
`java -cp target/webservice-0.1-SNAPSHOT.jar com.hortonworks.iotas.webservice.IotasApplication server conf/iotas.yaml`  

This should start the webserver on localhost port 8080. If you are running storm on the same host you may get 
`java.net.BindException: Address already in use` in which case you should modify `server` section of iotas.yaml.

##Intellij
`Run -> Edit Configuration -> Application -> IotasApplication` in the `Program argument section` add `server $IOTAS-HOME/webservice/conf/iotas.yaml`

Same config can be used to start debugging.

##Loading webserver with some data
`cd $IOTAS-HOME\bin`  
`./load-device.sh`  

This will load the following objects:  
`ParserInfo = ParserInfo{parserId=1, parserName='NestParser', className='com.hortonworks.iotas.parsers.nest.NestParser', jarStoragePath='/tmp/storm-0.1-SNAPSHOT.jar', parserSchema=null, version=0, timestamp=1439323206590}`  
`DataFeed = {"datafeedId":1,"datafeedName":"nest-datafeed","parserId":1,"endpoint":"localhost:9092/nest-topic"}`  
`DataSource = {"dataSourceName":"nest-datasource","description":"Nest as datasource","datafeedId":1,"tags":"thermostat, Google smart home"}`  
`Device = {"deviceId":"nest","version":1,"dataSourceId":1,"timestamp":1437938039136}`  

Please see `load-device.sh` which is just bunch of curl commands in case you want to add some other objects to webservice's inmemory store.

#Running storm topology
First you need to populate your kafka topic, if you have not done so create your kafka topic by executing    
`kafka-topics.sh --create --topic nest-topic --zookeeper localhost:2181 --replication-factor 1 --partitions 3`  

Then run the device simulator CLI to post some sample `IotasMessage` containing nest data to your kafka topic.  
`cd $$IOTAS-HOME`  
`java -cp simulator/target/simulator-0.1-SNAPSHOT.jar com.hortonworks.iotas.simulator.CLI -b localhost:9092 -t nest-topic -f simulator/src/main/resources/nest-iotas-messages`  

You can run the simulator command in a loop or run it multiple times to produce same data again and again.

Now you need to create hbase table where all the messages will be stored.  
`hbase shell`  
`create 'nest', 'cf'`

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

Now, From intellij you should be able to run `com.hortonworks.topology.IotasTopology` by providing `$IOTAS-HOME/storm/src/main/resources/topology.yaml` as argument and modifying `$IOTAS-HOME/storm/pom.xml` so `storm-core` is not in provided scope. 
you can also run the topology on a storm cluster by providing the name of the topology as second argument. RIGHT NOW THE TOPOLOGY DOES NOT EXECUTE IN A STORM CLUSTER AS THE JACKSON LIBRARY USED BY US HAVE A CONFLICTING
VERSION WITH STORM. Please merge https://github.com/apache/storm/pull/702 on your local storm cluster if you need to execute the topology on a storm cluster.

`storm jar $IOTAS-HOME/storm/target/storm-0.1-SNAPSHOT.jar com.hortonworks.topology.IotasTopology  $IOTAS-HOME/storm/src/main/resources/topology.yaml IotasTopology`  

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

#IoTaS topology using Flux
IoTaS topologies can also be built using FLUX now. For more information, 
please visit https://github.com/apache/storm/tree/master/external/flux

After doing a mvn package on IoTaS home directory you can run the following 
command `storm jar ./storm/target/storm-0.1-SNAPSHOT.jar org.apache.storm.flux.Flux --local --sleep 3600000 --filter ./storm/src/main/resources/flux_iotas_topology.properties ./storm/src/main/resources/flux_iotas_topology_config.yaml`

This will run the IOTaS topology in local mode for one hour, processing any events published to the 'nest-topic'. You can kill the topology anytime by pressing CNTL + C in the console.


