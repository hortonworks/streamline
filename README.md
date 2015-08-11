# IoTaS
Internet of Things at Scale.

##How to Run
From command line execute the following commands:

`cd $YOUR-IOTAS-ROOT-FOLDER`  
`mvn clean install`  
`cd webservice`  
`mvn package`  
`java -cp target/webservice-0.1-SNAPSHOT.jar com.hortonworks.iotas.webservice.IotasApplication server conf/iotas.yaml`  

This should start the webserver on localhost port 8080. If you are running storm on the same host you may get 
`java.net.BindException: Address already in use` in which case you should modify `server` section of iotas.yaml.

##Intellij
`Run -> Edit Configuration -> Application -> IotasApplication` in the `Prgoram argument section` add `server ${YOUR-IOTAS-ROOT}/webservice/conf/iotas.yaml`

Same config can be used to start debugging.

##Loading webserver with some data
`cd $YOUR-IOTAS-ROOT-FOLDER\bin`  
`./load-device.sh`  

This will load the following objects:  
`ParserInfo = ParserInfo{parserId=1, parserName='NestParser', className='com.hortonworks.parser.NestParser', jarStoragePath='/tmp/storm-0.1-SNAPSHOT.jar', parserSchema=null, version=0, timestamp=1439323206590}`  
`DataFeed = {"datafeedId":1,"datafeedName":"nest-datafeed","description":"Datafeed for Nest","tags":"Nest, ThermoStat","parserId":1,"endpoint":"localhost:9092/nest-topic","timestamp":1438055918158}`  
`DataSource = {"dataSourceName":"nest-datasource","description":"Nest as datasource","datafeedId":1,"tags":"thermostat, Google smart home"}`  
`Device = {"deviceId":"nest","version":1,"dataSourceId":1,"timestamp":1437938039136}`  

Please see `load-device.sh` which is just bunch of curl commands in case you want to add some other objects to webservice's inmemory store.

#Running storm topology
From intellij you should be able to run `com.hortonworks.topology.NestTopology` like any other topology.
