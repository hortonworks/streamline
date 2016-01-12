#Steps to setup IoTaS and run a sample topology on HDP sandbox.

Download HDP 2.3.2 sandbox for VirtualBox or VMware from http://hortonworks.com/hdp/downloads/

Follow the instructions for sandbox setup by clicking on the link for installation guide on the page in step above. For eg the steps for VirtualBox are 
listed here http://hortonworks.com/wp-content/uploads/2015/07/Import_on_Vbox_7_20_2015.pdf

ssh to sandbox machine using `ssh root@127.0.0.1 -p 2222` It requires you to setup a new password.

Once you successfully ssh into sandbox machine, make sure JAVA_HOME environment variable is defined. You can do this by `echo $JAVA_HOME` It should print 
 /usr/lib/jvm/java-1.7.0-openjdk-1.7.0.91.x86_64 If not set, please set and export the variable system wide so that it points to the correct jdk installation
 version.
 
Go to ambari server home page at http://127.0.0.1:8080 and login using username and password as admin

Using Ambari UI, stop all components on sandbox except for zookeeper, storm, kafka, hdfs and hbase. This can be done by going to http://127.0.0.1:8080/#/main/services/ and verifying the services on the list on the left hand side by taking necessary actions - stop/start.

At this point, you can clone the IoTaS git repository using your git username. Go to the terminal window with ssh session in to sandbox

`cd root`

`git clone https://<gitusername>@github.com/hortonworks/IoTaS.git`

`cd IoTaS`

Change directory to bin and run sandbox_setup.sh script

`cd bin`

`./sandbox_setup.sh`

Restart all storm components from ambari UI

At this point you are ready to deploy the iotas topology using storm by executing the below curl command

`curl -X POST http://localhost:21000/api/v1/catalog/topologies/1/actions/deploy -H "Content-Type: application/json"`

To kill the same topology execute the below curl command
`curl -X POST http://localhost:21000/api/v1/catalog/topologies/1/actions/kill -H "Content-Type: application/json"`



