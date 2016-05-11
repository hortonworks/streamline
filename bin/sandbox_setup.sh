#!/usr/bin/env bash
#Install apache storm using command line. 
cd /root
git clone https://github.com/apache/storm.git
cd storm
mvn clean install -DskipTests=true
#Following steps deal with starting up IoTaS server locally and running a topology
cd /root/IoTaS
#Copy the 3 files core-site.xml, hdfs-site.xml and hbase-site.xml to the directory inside IoTaS which is storm/src/main/resources. Note that this step is not needed to setup IoTaS server itself. However, to run a sample IoTaS topology that interacts with hbase and hdfs we need to package these files in the topology jar. The topology jar will be created when we compile IoTaS at top level. The commands for copying these files are as below.
cp /etc/hadoop/conf/core-site.xml /root/IoTaS/storm/src/main/resources/
cp /etc/hadoop/conf/hdfs-site.xml /root/IoTaS/storm/src/main/resources/
cp /etc/hbase/conf/hbase-site.xml /root/IoTaS/storm/src/main/resources/
mvn clean install -DskipTests=true
cp /root/IoTaS/storm/target/storm-0.1.0-SNAPSHOT.jar /tmp/
#Above step is to copy the storm jar that contains all components for running the topology to a location expected by IoTaS. Default location is /tmp. To change that, update the value for iotasStormJar in /root/IoTaS/webservice/conf/iotas.yaml and copy the storm jar to that location
#In /root/IoTaS/webservice/conf/iotas.yaml check the fileStorageConfiguration section. Update the fsUrl to the correct value from ambari hdfs configuration. By default the value for fsUrl is “hdfs://localhost:9000”. Sandbox ambari hdfs configuration by default uses the port 8020. Hence, the new value should be "hdfs://sandbox.hortonworks.com:8020" where sandbox.hortonworks.com is the hostname of the sandbox. Below is  the command to replace the value
sed -i 's/hdfs:\/\/localhost:9000/hdfs:\/\/sandbox.hortonworks.com:8020/g' /root/IoTaS/webservice/conf/iotas.yaml 
#Under the same configuration check the value for key directory and create hdfs directory with the same value. The is the directory that will be used by IoTaS for storing different jars in hdfs. You can use the default value of /tmp/test-hdfs or use a different value. The command for creating hdfs directory for default value are as below.
hdfs dfs -mkdir /tmp/test-hdfs
#Since the default port used by IoTaS to start the web server will conflict with ambari server port, execute the below command to update ports 8080 and 8081 to available ports. In this case available ports are 21000 and 21001. Also note that if you pick other ports than the ones mentioned here you might need to change port forwarding settings to be able to reach the IoTaS web server on sandbox from browser on the host running the sandbox.
sed -i 's/8080/21000/' /root/IoTaS/webservice/conf/iotas.yaml
sed -i 's/8081/21001/' /root/IoTaS/webservice/conf/iotas.yaml
#Now start the IoTaS web server by executing below commands
cd /root/IoTaS/webservice/
nohup java -cp target/webservice-0.1.0-SNAPSHOT.jar com.hortonworks.iotas.webservice.IotasApplication server conf/iotas.yaml&
sleep 10
#Once the web server has been successfully started you can quickly add the needed components to create and run a topology using a bunch of curl commands. However, because some of the topology components use configuration specific to the clusters on which those topology components will be running you need to follow the steps below to ensure that the component configurations have the correct value.
#Change the hdfs url.
sed -i 's/hdfs:\/\/localhost:9000/hdfs:\/\/sandbox.hortonworks.com:8020/g' /root/IoTaS/bin/topology 
#Change the port for IoTaS used by curl script load-device.sh
sed -i 's/8080/21000/' /root/IoTaS/bin/load-device.sh
cd /root/IoTaS/bin/
./load-device.sh 
#Note that you might get an error response for notifiers endpoint. You can ignore that for now.
#Create kafka topic from which the topology consumes the data
/usr/hdp/2.3.2.0-2950/kafka/bin/kafka-topics.sh --create --topic nest-topic --zookeeper localhost:2181 --replication-factor 1 --partitions 3
#Produce some data on to the topic just created above. Note that hostname for kafka broker should be the hostname of sandbox VM and the port for kafka broker should be the same as the one configured in ambari for kafka. 
java -cp /root/IoTaS/simulator/target/simulator-0.1.0-SNAPSHOT.jar com.hortonworks.iotas.simulator.CLI -b sandbox.hortonworks.com:6667 -t nest-topic -f /root/IoTaS/simulator/src/main/resources/nest-iotas-messages
#Create the hbase table used by topology to save tuples satisfying rules.
echo "create 'nest', 'cf', 'd'" | hbase shell
#Create the hdfs directories used by the components.
hdfs dfs -mkdir /tmp/hbase
hdfs dfs -mkdir /tmp/failed-tuples
#Note that the above two directories are the default used in the topology created using load-device.sh. If the default values have been changed then those directories need to be created
#Grant write permissions to hdfs directories created for user with which the bolt of the underlying storm topology will be run. That user is usually storm. Below commands grant write permission to all users 
hdfs dfs -chmod 777 /tmp/hbase
hdfs dfs -chmod 777 /tmp/failed-tuples
#Before submitting the topology, you need to copy apache storm core jars with correct version to the hdp directory on sandbox. Reason being pom.xml for topology jar has dependencies declared on apache storm-core in provided scope. The storm-core jars on sandbox are from HDP distribution. Below are the steps to copy the right jars to avoid any exceptions while running the topology. For the topology in consideration the version is 0.10.0-beta1. 
cd /root
wget http://www.us.apache.org/dist/storm/apache-storm-0.10.0-beta1/apache-storm-0.10.0-beta1.tar.gz
tar xvf apache-storm-0.10.0-beta1.tar.gz
mkdir /tmp/lib.old
find /usr/hdp/current/storm-nimbus/lib/ -type f | xargs -I '{}' mv {} /tmp/lib.old/
cp /root/apache-storm-0.10.0-beta1/lib/* /usr/hdp/current/storm-nimbus/lib/
