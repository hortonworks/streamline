# How to add a spout as a custom source

If you already have a spout implemented and want to use it in a SAM application, SAM lets you register it once  and re-use in any SAM application. This 
way, you can avoid rewriting code and leverage existing spout code. Provided below, is an illustration of how to build and register such a spout as a SAM source. 
All the files needed to build, register and run are present in this module.

1. Check out the module on your local directory and run `mvn clean install`
2. Open the `file-source-topology-component.json` file under resources directory and make sure that the mavenDeps property(which is of the form 
groupId:artifactId:version) has the correct value for version. Version should be the version defined in the pom file of this module
3. Export the variable `data`. Its value should be the location of the json file edited in step 2 above.

`export data=/tmp/file-source-topology-component.json`

4. Run the below curl command to register it as a SAM source. Before doing that, make sure you replace sam_host and sam_port with values for your environment
 and that bundleJar form parameter points to the jar file that you build in step 1. 

`curl -sS -X POST -i -F topologyComponentBundle=@$data  -F bundleJar=@/tmp/custom_source_example/streamline-examples-sources-0.1.0-SNAPSHOT.jar http://<sam_host>:<sam_port>/api/v1/catalog/streams/componentbundles/SOURCE`

5. Install the jar built in step 1 above in local m2 directory for the user running SAM server on SAM server using the command below. Make sure the version 
used is the correct one from the pom file of this project

`mvn install:install-file -DgroupId=com.hortonworks.streamline -DartifactId=streamline-examples-sources -Dpackaging=jar -Dversion=0.1.0-SNAPSHOT -Dfile=/tmp/streamline-examples-sources-0.1.0-SNAPSHOT.jar -DgeneratePom=true`

6. At this point, you are ready to drag and drop this source on to a SAM app. Note that this source reads from a file on local filesystem and it expects the 
file to be in a certain format. Sample file provided in this module names.csv under resources directory can be used. Please make sure that the value for path 
configuration parameter for this source points to the location of names.csv or a similar file in the same format on storm supervisor nodes.

