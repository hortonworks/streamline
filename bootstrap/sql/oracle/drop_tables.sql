CREATE OR REPLACE PROCEDURE drop_if_exists( object_type IN VARCHAR2, object_name IN VARCHAR2 ) AS
BEGIN
   IF (object_type='TABLE') THEN
      EXECUTE IMMEDIATE 'DROP ' || object_type || ' "' || object_name || '" CASCADE CONSTRAINTS';
   ELSIF (object_type='SEQUENCE') THEN
      EXECUTE IMMEDIATE 'DROP ' || object_type || ' "' || object_name || '"';
   END IF;
EXCEPTION
   WHEN OTHERS THEN
      IF (object_type='TABLE' AND SQLCODE != -942) OR (object_type='SEQUENCE' AND SQLCODE != -2289) THEN
         RAISE;
      END IF;
END drop_if_exists;

#

CALL drop_if_exists('TABLE','file')#
CALL drop_if_exists('TABLE','tag')#
CALL drop_if_exists('TABLE','tag_storable_map')#
CALL drop_if_exists('TABLE','topology_component_bundle')#
CALL drop_if_exists('TABLE','notifier')#
CALL drop_if_exists('TABLE','topology_test_run_histories')#
CALL drop_if_exists('TABLE','topology_test_run_case')#
CALL drop_if_exists('TABLE','topology_component')#
CALL drop_if_exists('TABLE','topology_source_stream_map')#
CALL drop_if_exists('TABLE','topology_source')#
CALL drop_if_exists('TABLE','topology')#
CALL drop_if_exists('TABLE','topology_editor_metadata')#
CALL drop_if_exists('TABLE','topology_editor_toolbar')#
CALL drop_if_exists('TABLE','topology_sink')#
CALL drop_if_exists('TABLE','topology_processor_stream_map')#
CALL drop_if_exists('TABLE','topology_processor')#
CALL drop_if_exists('TABLE','topology_edge')#
CALL drop_if_exists('TABLE','topology_rule')#
CALL drop_if_exists('TABLE','topology_branchrule')#
CALL drop_if_exists('TABLE','topology_window')#
CALL drop_if_exists('TABLE','topology_stream')#
CALL drop_if_exists('TABLE','topology_version')#
CALL drop_if_exists('TABLE','udf')#
CALL drop_if_exists('TABLE','service_configuration')#
CALL drop_if_exists('TABLE','component')#
CALL drop_if_exists('TABLE','cluster')#
CALL drop_if_exists('TABLE','service')#
CALL drop_if_exists('TABLE','namespace')#
CALL drop_if_exists('TABLE','namespace_service_cluster_map')#
CALL drop_if_exists('TABLE','widget_datasource_map')#
CALL drop_if_exists('TABLE','widget')#
CALL drop_if_exists('TABLE','datasource')#
CALL drop_if_exists('TABLE','ml_model')#
CALL drop_if_exists('TABLE','dashboard')#
CALL drop_if_exists('TABLE','topology_state')#
CALL drop_if_exists('TABLE','service_bundle')#
CALL drop_if_exists('TABLE','acl_entry')#
CALL drop_if_exists('TABLE','role_hierarchy')#
CALL drop_if_exists('TABLE','user_role')#
CALL drop_if_exists('TABLE','role')#
CALL drop_if_exists('TABLE','user_entry')#
CALL drop_if_exists('TABLE','topology_test_run_case_sink')#
CALL drop_if_exists('TABLE','topology_test_run_case_source')#


CALL drop_if_exists('SEQUENCE','DASHBOARD')#
CALL drop_if_exists('SEQUENCE','ML_MODEL')#
CALL drop_if_exists('SEQUENCE','WIDGET')#
CALL drop_if_exists('SEQUENCE','DATASOURCE')#
CALL drop_if_exists('SEQUENCE','FILE')#
CALL drop_if_exists('SEQUENCE','NAMESPACE')#
CALL drop_if_exists('SEQUENCE','TOPOLOGY_VERSION')#
CALL drop_if_exists('SEQUENCE','TOPOLOGY')#
CALL drop_if_exists('SEQUENCE','TOPOLOGY_COMPONENT_BUNDLE')#
CALL drop_if_exists('SEQUENCE','TAG')#
CALL drop_if_exists('SEQUENCE','TOPOLOGY_STREAM')#
CALL drop_if_exists('SEQUENCE','NOTIFIER')#
CALL drop_if_exists('SEQUENCE','TOPOLOGY_COMPONENT')#
CALL drop_if_exists('SEQUENCE','TOPOLOGY_EDGE')#
CALL drop_if_exists('SEQUENCE','TOPOLOGY_RULE')#
CALL drop_if_exists('SEQUENCE','TOPOLOGY_BRANCHRULE')#
CALL drop_if_exists('SEQUENCE','TOPOLOGY_WINDOW')#
CALL drop_if_exists('SEQUENCE','UDF')#
CALL drop_if_exists('SEQUENCE','CLUSTER')#
CALL drop_if_exists('SEQUENCE','SERVICE')#
CALL drop_if_exists('SEQUENCE','SERVICE_CONFIGURATION')#
CALL drop_if_exists('SEQUENCE','COMPONENT')#
CALL drop_if_exists('SEQUENCE','TOPOLOGY_STATE')#
CALL drop_if_exists('SEQUENCE','SERVICE_BUNDLE')#
CALL drop_if_exists('SEQUENCE','ACL_ENTRY')#
CALL drop_if_exists('SEQUENCE','ROLE')#
CALL drop_if_exists('SEQUENCE','TOPOLOGY_EDITOR_TOOLBAR')#
CALL drop_if_exists('SEQUENCE','USER_ENTRY')#
CALL drop_if_exists('SEQUENCE','TOPOLOGY_TEST_RUN_CASE')#
CALL drop_if_exists('SEQUENCE','TOPOLOGY_TEST_RUN_CASE_SOURCE')#
CALL drop_if_exists('SEQUENCE','TOPOLOGY_TEST_RUN_CASE_SINK')#
CALL drop_if_exists('SEQUENCE','TOPOLOGY_TEST_RUN_HISTORIES')#

