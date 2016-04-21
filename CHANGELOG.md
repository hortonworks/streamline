## 0.1.2
 * IOT-224: Create tags on the fly for datasources for backward compatibility.
 * IOT-201: Integrate storm-sql UDF in Iotas.
 * IOT-99 : Implement hierarchical tags for IoTaS
 * IOT-116: Parser Exception handling requirements.
 * IOT-192: Added support for templatized message.
 * IOT-172: Fix parser file name conflict.
 * IOT-206: Generate normalization processor related components required for flux yaml and UI.
 * IOT-86: Added api to query topology status.
 * IOT-191: UI - topology actions should be integrated and provide view mode and edit mode on topology editor
 * IOT-210: UI - Drop down for configs should be just move to Control Panel
 * IOT-211: UI - Add breadcrumbs navigation bar for all the pages.
 * IOT-199: UI - Improve the componen icon status to show its not configured
 * IOT-217: Refactor to keep source, sink and processor interfaces independent
 * IOT-173: Parser upload should throw a error back to user if the specified class is not available in the jar.
 * IOT-174: Improve Parser upload experience.
 * IOT-203: Make configKey to not be a user input for hbase bolt.
 * IOT-176: topology editor deploy fails if stormHomeDir lacks leading /
 * IOT-147: Support array and nested field lookup with Storm Sql.
 * IOT-177: Email notification bolt is throwing IllegalStateException.
 * IOT-175: Fix generics in Parser.
 * IOT-64:  Added normalization processor for bulk/field level normalization with groovy scripts.
 * IOT-171: Normalization processor/bolt contract

## 0.1.1
 * IOT-135: Implement Custom Processors.
 * IOT-131: Added common Config object.
 * IOT-128: Refactored design time entities.
 * IOT-148: Topology Editor should be able delete the nodes.
 * IOT-98: Add pair of parser name and version unique constraint in parser info.
 * IOT-149: Topology Editor should allow multiples rules through UI.
 * IOT-154: Fix package names to comply with the pattern com.hortonworks.iotas.
 * IOT-32:  Make Logging frameworks coherent across libraries and IOT codebase.
 * IOT-63:  scriptEngine.eval(scriptText) is run every tuple which is heavy and it should be avoided.
 * IOT-78:  Add dataset(weather, twittter firehouse.. etc) to IoT.
 * IOT-112: DummyRuleBolt should be renamed and moved to test package.
 * IOT-113: Typos in error message thrown while deploying topology.
 * IOT-133: create iotas-dist, cleanup artifactid, start/stop script.
 * IOT-137: Support shuffle mechanisms based on stream processing framework.
 * IOT-152: Fix rpm build issues.
 * IOT-159: Topology Editor changes.
 * IOT-162: UI - connecting Parser to sink should be possible.
 * IOT-163: UI - Any modal thats open should be closed by esc key.
 * IOT-178: UI - Support Nested Json.
 * IOT-179: UI - Enable custom processors registration and using them in topology editor.
 * IOT-195: Custom Processor in topology editor.
