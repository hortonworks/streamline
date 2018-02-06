# How to add a spout as a custom source

If you already have a spout implemented and want to use it in a SAM application, SAM lets you register it once  and re-use in any SAM application. This 
way, you can avoid rewriting code and leverage existing spout code. Provided below, is an illustration of how to build and register such a spout as a SAM source. 
All the files needed to build, register and run are present in this module.

1. Check out the module on your local directory and run `mvn clean install`
2. Open the `file-source-topology-component.json` file under resources directory and delete the mavenDeps property.
3. Export the variable `data`. Its value should be the location of the json file edited in step 2 above.

`export data=/tmp/file-source-topology-component.json`

4. Run the below curl command to register it as a SAM source. Before doing that, make sure you replace sam_host and sam_port with values for your environment
 and that bundleJar form parameter points to the jar file that you build in step 1. 

`curl -sS -X POST -i -F topologyComponentBundle=@$data  -F bundleJar=@/tmp/custom_source_example/streamline-examples-sources-0.6.0-SNAPSHOT.jar http://<sam_host>:<sam_port>/api/v1/catalog/streams/componentbundles/SOURCE`

5. At this point, you are ready to drag and drop this source on to a SAM app. Note that this source reads from a file on local filesystem and it expects the 
file to be in a certain format. Sample file provided in this module names.csv under resources directory can be used. Please make sure that the value for path 
configuration parameter for this source points to the location of names.csv or a similar file in the same format on storm supervisor nodes.

Note that in case of this example, the spout code is included in the module and the jar built. However, if spout code is not a part of the jar uploaded in 
the curl call in step 4 then you will need to update the mavenDeps property in step 2 instead of deleting it. It is of the form groupId:artifactId:version. 
The jar file containing the spout and all its dependencies should be available as a maven artifact at one of the repositories specified as mavenRepoUrl in 
streamline yaml configuration. If its not present at a remote repository, you will need to install the jar in local m2 directory for the user running SAM server
on SAM server using the command below. Please make sure to change the groupId, artifactId, version and the file arguments as per your pom file and mavenDeps

`mvn install:install-file -DgroupId=com.hortonworks.streamline -DartifactId=streamline-examples-sources -Dpackaging=jar -Dversion=0.6.0-SNAPSHOT -Dfile=/tmp/streamline-examples-sources-0.6.0-SNAPSHOT.jar -DgeneratePom=true`
