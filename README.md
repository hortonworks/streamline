# IoTaS
Internet of Things at Scale.

##How to Run
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
`DataFeed = {"datafeedId":1,"datafeedName":"nest-datafeed","description":"Datafeed for Nest","tags":"Nest, ThermoStat","parserId":1,"endpoint":"localhost:9092/nest-topic","timestamp":1438055918158}`  
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

Now, From intellij you should be able to run `com.hortonworks.topology.IotasTopology` by providing `$IOTAS-HOME/storm/src/main/resources/topology.yaml` as argument and modifying `$IOTAS-HOME/storm/pom.xml` so `storm-core` is not in provided scope. 
you can also run the topology on a storm cluster by providing the name of the topology as second argument. RIGHT NOW THE TOPOLOGY DOES NOT EXECUTE IN A STORM CLUSTER AS THE JACKSON LIBRARY USED BY US HAVE A CONFLICTING
VERSION WITH STORM. Please merge https://github.com/apache/storm/pull/702 on your local storm cluster if you need to execute the topology on a storm cluster.

`storm jar $IOTAS-HOME/storm/target/storm-0.1-SNAPSHOT.jar com.hortonworks.topology.IotasTopology  $IOTAS-HOME/storm/src/main/resources/topology.yaml IotasTopology`  

#Accounting for bad tuples in a topology
A mechanism has been added so that when messages are being played from a 
spout in an IotaS topology and they cant successfully be parsed then such 
messages end up in some persistent storage. The mechanism has been abstracted
in to an interface called `com.hortonworks.topology.UnparsedTupleHandler`. A 
default hdfs based implementation for the interface has been provided in the 
class com.hortonworks.topology.HdfsUnparsedTupleHandler. To be able to use 
that implementation a ParserBolt just needs to be supplied with an object of 
that implementation. Please see usage below. This will write all the tuples 
to hdfs with fsUrl, path and name of the file as mentioned. Note that it will
append a UUID to the name of the file. So if one were to search for all the 
files containing tuples that failed parsing, they would need to search with 
a regex like data* instead of just data in the hdfs path specified. By 
default the records are separated by \n. However you can choose to specify 
another record delimiter character using withRecordDelimiter method.

`UnparsedTupleHandler unparsedTupleHandler = new`
    `HdfsUnparsedTupleHandler().withFsUrl("hdfs://localhost" +`
    `":9000").withPath("/failed-tuples").withName("data");`
`parserBolt.withUnparsedTupleHandler(unparsedTupleHandler);`

#Accessing UI
http://localhost:8080/ui/index.html

#IoTaS topology using Flux
IoTaS topologies can also be built using FLUX now. For more information, 
please visit https://github.com/apache/storm/tree/master/external/flux

After doing a mvn package on IoTaS home directory you can run the following 
command `storm jar ./storm/target/storm-0.1-SNAPSHOT.jar org.apache.storm.flux.Flux --local --filter ./storm/src/main/resources/flux_iotas_topology.properties ./storm/src/main/resources/flux_iotas_topology_config.yaml`
