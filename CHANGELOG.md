## 0.1.4
 * STREAMLINE-394: Remove classes related to device centric view. 
 * STREAMLINE-393: Capture Metadata from clusters.
 * STREAMLINE-444: Enhance UDF api include type information.
 * STREAMLINE-448: Windowing component should add topology.message.timeout.secs.
 * STREAMLINE-460: Rename packages from com.hortonworks.iotas to org.apache.streamline
 * STREAMLINE-392: ClusterConfiguration should be able to take in ambari url and grab Storm, Kafka , HDFS , Hbase, Hive etc.. configurations
 * STREAMLINE-441: Remove creating tables as part of server start
 * STREAMLINE-396: Enhance UDF support in rules engine.
 * STREAMLINE-359: Move Windowing its own component.
 * STREAMLINE-390: Added option to directly specify conditions in rule api.
 * STREAMLINE-429: Topology Editor component panel changes.
 * STREAMLINE-425: Topology Editor Improvements.
 * STREAMLINE-408: Remove the extra config property in topology level config json.
 * STREAMLINE-407: Remove imageFile field for custom processors.
 * STREAMLINE-389: A few fixes for issues found while running with mysql db
 * STREAMLINE-373: Email Notifier throws IllegalStateException
 * STREAMLINE-391: Add Eventhub source example in topology rest api script
 * STREAMLINE-397: UI bug fixes and changes for module refactoring 
 * STREAMLINE-398: Fix topology dag generation issue due to processor output streams not being populated
 * STREAMLINE-326: TopologyDagVisitor to have visit methods for IotasSource/IotasSink/IotasProcessor
 * STREAMLINE-368: Send empty array response in GET ALL requests.
 * STREAMLINE-389: Added missing ruleinfo and udf tables.
 * STREAMLINE-387: Misc fixes including Harsha's fixes to be able to deploy topologies.
 * STREAMLINE-287: UI Refactoring
 * STREAMLINE-356: Add Kafka, Kinesis, EventHubSpout as sources
 * STREAMLINE-369: Make InMemoryStorageManager getNextId thread safe
 * STREAMLINE-352: Remove core module and a couple other JIRAs
 * STREAMLINE-347: Fix bootstrap.sh so it can be run from anywhere
 * STREAMLINE-352: Refactor core module to move components into streams and other modules
 * STREAMLINE_344: Iotas build fails at storage-atlas with error
 * STREAMLINE-285: Hierarchical tagging system should be independent registry service
 * STREAMLINE-279: Add clean up logic if any part of custom processor upload fails.
 * STREAMLINE-346: Remove metrics dependency from webservice.
 * STREAMLINE-343: UI Sets Parallelism Property to Type String Which Causes Topology Deployment Failure.
 * STREAMLINE-266: Added OpenTsdb sink implementation using Storm's OpenTsdb bolt.
 * STREAMLINE-311: Support specifying output stream within source/processor apis
 * STREAMLINE-342: Rename strings in streams-layout-storm module
 * STREAMLINE-295: Move classes to streams-runtime-storm
 * STREAMLINE-294: Refactor classes from layout to streams-runtime
 * STREAMLINE-341: Build failing due to unit test failures in storage-atlas module
 * STREAMLINE-340: Move Cache to a separate module
 * STREAMLINE-315: RulesChannelHandler does not override abstract method setSource
 * STREAMLINE-317: Create streams-sdk
 * STREAMLINE-282: Atlas storage manager implementation
 * STREAMLINE-293: Streams: create streams-service
 * STREAMLINE-280: catalog refactoring
 * STREAMLINE-297: Move metrics to the streams project
 * STREAMLINE-307: Create streams-layout-storm
 * STREAMLINE-292: Create streams-layout
 * STREAMLINE-306: Create new storage module for refactoring 
 * STREAMLINE 296: Refactor notifers and notification modules to move to the streams project
 * STREAMLINE-289: Streams: create a top level streams and the mvn sub project skelton
 * STREAMLINE-267: Intermittent Unit test failure in WindowRulesBoltTest
 * STREAMLINE-304: integrate normalization components with new topology DAG APIs
 * STREAMLINE-273: Iotas rule processor api sql support
 * STREAMLINE-303: Notifications and existing action in rules should use new Action abstraction introduced as part of split/join implementation.
 * STREAMLINE-302: split/join processor to be integrated with topology dag component APIs
 * STREAMLINE-301: Integrate split - stage - join processor in topology editor
 * STREAMLINE-299: Add 'null' check for OpenTSDBWithStormQuerier
 * STREAMLINE-188: Added split/join/stage implementation for runtime
 * STREAMLINE-274: Introduce REST API: Time-series DB querier
 * STREAMLINE-155: Convert Topology UI JSON to Design time entities and construct DAG
 * STREAMLINE-161: REST api for source, sink, processor and edge
 * STREAMLINE-119: STREAMLINE Cache Requirements
 
## 0.1.3
 * STREAMLINE-269: Normalization processor runtime and UI integration
 * STREAMLINE-239: TP-3 release bug fixes
 * STREAMLINE-247: Add integration test for tagging
 * STREAMLINE-8: Sliding Time Windows
 * STREAMLINE-252: Add integration test for StreamInfo storage with JDBC provider
 * STREAMLINE-261: topology status API can't determine status of topology on Storm
 * STREAMLINE-262: Fix tagging api throwing java.lang.StackOverflowError: null
 * STREAMLINE-259: Do not throw an exception if schema is null
 * STREAMLINE-256: Hierarchical tagging UI integration
 * STREAMLINE-255: UI integration to improve parser upload experience
 * STREAMLINE-265: Topologies with notification sinks throw error while deploying to storm 
 * STREAMLINE-248: Integration tests for Notification Service
 * STREAMLINE-189: Support adding custom artifacts in storm.jar
 * STREAMLINE-251: Multiple parser bolts can over write the same loaded parser jars leading to classloading issues.
 * STREAMLINE-250: REST api for output streams
 * STREAMLINE-158: UI should provide functionality to select a shuffle
 
## 0.1.2
 * STREAMLINE-226: Create file resource(including jars) utility to be used in any component of a topology
 * STREAMLINE-238: Fixed Tag and TagStorableMapping to be created as Storables in JDBC/Phoenix storgae providers
 * STREAMLINE-196: Phoenix storage provider support in webservices modules.
 * STREAMLINE-231: Change device id and version to make and model.
 * STREAMLINE-215: Introduce new REST API to show metrics for topology.
 * STREAMLINE-170: Add file watcher and custom processor file watcher handler with unit tests.
 * STREAMLINE-224: Create tags on the fly for datasources for backward compatibility.
 * STREAMLINE-201: Integrate storm-sql UDF in Iotas.
 * STREAMLINE-99 : Implement hierarchical tags for IoTaS
 * STREAMLINE-116: Parser Exception handling requirements.
 * STREAMLINE-192: Added support for templatized message.
 * STREAMLINE-172: Fix parser file name conflict.
 * STREAMLINE-206: Generate normalization processor related components required for flux yaml and UI.
 * STREAMLINE-86: Added api to query topology status.
 * STREAMLINE-191: UI - topology actions should be integrated and provide view mode and edit mode on topology editor
 * STREAMLINE-210: UI - Drop down for configs should be just move to Control Panel
 * STREAMLINE-211: UI - Add breadcrumbs navigation bar for all the pages.
 * STREAMLINE-199: UI - Improve the componen icon status to show its not configured
 * STREAMLINE-217: Refactor to keep source, sink and processor interfaces independent
 * STREAMLINE-173: Parser upload should throw a error back to user if the specified class is not available in the jar.
 * STREAMLINE-174: Improve Parser upload experience.
 * STREAMLINE-203: Make configKey to not be a user input for hbase bolt.
 * STREAMLINE-176: topology editor deploy fails if stormHomeDir lacks leading /
 * STREAMLINE-147: Support array and nested field lookup with Storm Sql.
 * STREAMLINE-177: Email notification bolt is throwing IllegalStateException.
 * STREAMLINE-175: Fix generics in Parser.
 * STREAMLINE-64:  Added normalization processor for bulk/field level normalization with groovy scripts.
 * STREAMLINE-171: Normalization processor/bolt contract

## 0.1.1
 * STREAMLINE-135: Implement Custom Processors.
 * STREAMLINE-131: Added common Config object.
 * STREAMLINE-128: Refactored design time entities.
 * STREAMLINE-148: Topology Editor should be able delete the nodes.
 * STREAMLINE-98: Add pair of parser name and version unique constraint in parser info.
 * STREAMLINE-149: Topology Editor should allow multiples rules through UI.
 * STREAMLINE-154: Fix package names to comply with the pattern com.hortonworks.iotas.
 * STREAMLINE-32:  Make Logging frameworks coherent across libraries and STREAMLINE codebase.
 * STREAMLINE-63:  scriptEngine.eval(scriptText) is run every tuple which is heavy and it should be avoided.
 * STREAMLINE-78:  Add dataset(weather, twittter firehouse.. etc) to IoT.
 * STREAMLINE-112: DummyRuleBolt should be renamed and moved to test package.
 * STREAMLINE-113: Typos in error message thrown while deploying topology.
 * STREAMLINE-133: create iotas-dist, cleanup artifactid, start/stop script.
 * STREAMLINE-137: Support shuffle mechanisms based on stream processing framework.
 * STREAMLINE-152: Fix rpm build issues.
 * STREAMLINE-159: Topology Editor changes.
 * STREAMLINE-162: UI - connecting Parser to sink should be possible.
 * STREAMLINE-163: UI - Any modal thats open should be closed by esc key.
 * STREAMLINE-178: UI - Support Nested Json.
 * STREAMLINE-179: UI - Enable custom processors registration and using them in topology editor.
 * STREAMLINE-195: Custom Processor in topology editor.
