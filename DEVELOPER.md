# Developer documentation

This document summarizes information relevant to Streamline committers and contributors.  It includes information about
the development processes and policies as well as the tools we use to facilitate those.

---

Table of Contents

* <a href="#workflow-and-policies">Workflows</a>
    * <a href="#coding-guidelines">Coding Guidelines</a>
    * <a href="#building">Build the code and run the tests</a>
* <a href="#run-local">Run Locally</a>
    * <a href="#run-local-instance">Run Local Instance</a>
    * <a href="#run-intellij"> Run instance through Intellij</a>

---

# Workflows

This section explains how to perform common activities such as reporting a bug or merging a pull request.

<a name="coding-guidelines"></a>

## Coding Guidelines

### Basic

 1. Avoid cryptic abbreviations. Single letter variable names are fine in very short methods with few variables, otherwise make them informative.
 2. Clear code is preferable to comments. When possible make your naming so good you don't need comments. When that isn't possible comments should be thought of as mandatory, write them to be read.
 3. Logging, configuration, and public APIs are our "UI". Make them pretty, consistent, and usable.
 4. Maximum line length is 130.
 5. Don't leave TODOs in the code or FIXMEs if you can help it. Don't leave println statements in the code. TODOs should be filed as JIRAs.
 6. User documentation should be considered a part of any user-facing the feature, just like unit tests. Example REST apis should've accompanaying documentation.
 7. Tests should never rely on timing in order to pass.  
 8. Every unit test should leave no side effects, i.e., any test dependencies should be set during setup and clean during tear down.

### Java
 1. Apache license headers. Make sure you have Apache License headers in your files. 
 2. Tabs vs. spaces. We are using 4 spaces for indentation, not tabs. 
 3. Blocks. All statements after if, for, while, do, … must always be encapsulated in a block with curly braces (even if the block contains one statement):
     for (...) {
         ...
     }
 4. No wildcard imports. 
 5. No unused imports. Remove all unused imports.
 6. No raw types. Do not use raw generic types, unless strictly necessary (sometime necessary for signature matches, arrays).
 7. Suppress warnings. Add annotations to suppress warnings, if they cannot be avoided (such as “unchecked”, or “serial”).
 8. Comments.  Add JavaDocs to public methods or inherit them by not adding any comments to the methods. 
 9. logger instance should be upper case LOG.  
 10. When in doubt refer to existing code or <a href="http://google.github.io/styleguide/javaguide.html"> Java Coding Style </a> except line breaking, which is described above. 
  

### Logging

 1. Please take the time to assess the logs when making a change to ensure that the important things are getting logged and there is no junk there.
 2. There are six levels of logging TRACE, DEBUG, INFO, WARN, ERROR, and FATAL, they should be used as follows.
    
    ``` 
    2.1 INFO is the level you should assume the software will be run in. 
     INFO messages are things which are not bad but which the user will definitely want to know about
     every time they occur.
    
    2.2 TRACE and DEBUG are both things you turn on when something is wrong and you want to figure out 
    what is going on. DEBUG should not be so fine grained that it will seriously effect the performance 
    of the server. TRACE can be anything. Both DEBUG and TRACE statements should be 
    wrapped in an if(logger.isDebugEnabled) if an expensive computation in the argument list of log method call.
    
    2.3 WARN and ERROR indicate something that is bad. Use WARN if you aren't totally sure it is bad,
     and ERROR if you are.
    
    2.4 Use FATAL only right before calling System.exit().
    ```
 3. Logging statements should be complete sentences with proper capitalization that are written to be read by a person not necessarily familiar with the source code. 
 4. String appending using StringBuilders should not be used for building log messages. 
    Formatting should be used. For ex:
    LOG.debug("Loaded class [{}] from jar [{}]", className, jarFile);
 
<a name="building"></a>

# Build the code and run the tests

## Prerequisites
Firt of all you need to make sure you are using maven 3.2.5 or higher and JDK 1.8 or higher.

## Building

The following commands must be run from the top-level directory.

`mvn clean install`

If you wish to skip the unit tests you can do this by adding `-DskipTests` to the command line. 

## Create a distribution (packaging)

You can create a _distribution_ as follows.

    # First, build the code.
    # Pivot module required for the dist package to be built , Its not run as part of default build.
    # To build pivot along with all the other modules.     $ mvn clean install -Pall 

    # Create the binary distribution.
    $ cd streamline-dist && mvn package

You can also use the maven `dist` profile to build the code and create the distribution in one step.

    $ mvn clean install -P dist 

The binaries will be created at:

    streamline-dist/target/hortonworks-streamline-<version>.pom
    streamline-dist/target/hortonworks-streamline-<version>.tar.gz
    streamline-dist/target/hortonworks-streamline-<version>.zip

including corresponding `*.asc` digital signature files.

After running `mvn package` you may be asked to enter your GPG/PGP credentials (once for each binary file, in fact).
This happens because the packaging step will create `*.asc` digital signatures for all the binaries, and in the workflow
above _your_ GPG private key will be used to create those signatures.

You can verify whether the digital signatures match their corresponding files:

    # Example: Verify the signature of the `.tar.gz` binary.
    $ gpg --verify streamline-dist/target/hortonworks-streamline-<version>.tar.gz.asc

<a name="run-local"></a>
 
# Run Locally
 
## Prerequisites

Make sure you followed [Create a distribution](#building) steps to create binary dist package.

<a name="run-local-instance"></a>
## Run Local Instance

1. unzip hortonworks-streamline-{project.verson}.zip
3. cd hortonworks-streamline-{project.version}
4. Edit conf/streamline.yaml and provide the database and credentials under the "storageProviderConfiguration".
5. If you don't want to run a DB locally , you can use InMemoryStorageManager. You'll loose any changes if you restart the server
6. ./bootstrap/bootstrap-storage.sh migrate
7. ./bin/streamline start
8. ./bootstrap/bootstrap.sh migrate
9. You should be able to access streamline by visiting http://HOST:8080/

<a name="run-intellij"></a>
## Run instance through Intellij

1. Add new Run/Debug configuration like below screenshot. 
   ![Intellj Run Configuration](docs/images/intellij-conf.png?raw=True)
2. If you are going to use mysql as your DB, make sure you add mysql connector jar "“Project settings -> Libraries”."
