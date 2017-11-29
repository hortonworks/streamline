-- Copyright 2017 Hortonworks.
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--    http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

-- Drop all foreign keys which doesn't have explicit symbol

DROP PROCEDURE IF EXISTS dropForeignKeysFromTable;

delimiter ///
create procedure dropForeignKeysFromTable(IN param_table_name varchar(255))
begin
    declare done int default FALSE;
    declare dropCommand varchar(255);
    declare dropCur cursor for
        select concat('alter table ',table_schema,'.',table_name,' DROP FOREIGN KEY ',constraint_name, ';')
        from information_schema.table_constraints
        where constraint_type='FOREIGN KEY'
            and table_name = param_table_name
            and table_schema = database();

    declare continue handler for not found set done = true;

    open dropCur;

    read_loop: loop
        fetch dropCur into dropCommand;
        if done then
            leave read_loop;
        end if;

        set @sdropCommand = dropCommand;

        prepare dropClientUpdateKeyStmt from @sdropCommand;

        execute dropClientUpdateKeyStmt;

        deallocate prepare dropClientUpdateKeyStmt;
    end loop;

    close dropCur;
end///

delimiter ;

CALL dropForeignKeysFromTable('widget');
CALL dropForeignKeysFromTable('datasource');
CALL dropForeignKeysFromTable('widget_datasource_map');
CALL dropForeignKeysFromTable('topology');
CALL dropForeignKeysFromTable('topology_editor_metadata');
CALL dropForeignKeysFromTable('topology_stream');
CALL dropForeignKeysFromTable('topology_source');
CALL dropForeignKeysFromTable('topology_source_stream_map');
CALL dropForeignKeysFromTable('topology_sink');
CALL dropForeignKeysFromTable('topology_processor');
CALL dropForeignKeysFromTable('topology_processor_stream_map');
CALL dropForeignKeysFromTable('topology_edge');
CALL dropForeignKeysFromTable('topology_rule');
CALL dropForeignKeysFromTable('topology_branchrule');
CALL dropForeignKeysFromTable('topology_window');
CALL dropForeignKeysFromTable('service');
CALL dropForeignKeysFromTable('service_configuration');
CALL dropForeignKeysFromTable('component');
CALL dropForeignKeysFromTable('component_process');
CALL dropForeignKeysFromTable('role_hierarchy');
CALL dropForeignKeysFromTable('user_role');
CALL dropForeignKeysFromTable('topology_editor_toolbar');
CALL dropForeignKeysFromTable('topology_test_run_case');
CALL dropForeignKeysFromTable('topology_test_run_case_source');
CALL dropForeignKeysFromTable('topology_test_run_case_sink');
CALL dropForeignKeysFromTable('topology_test_run_histories');

-- add foreign keys with explicit symbol
ALTER TABLE widget ADD CONSTRAINT `fk_widget_dashboard` FOREIGN KEY (dashboardId) REFERENCES dashboard(id);
ALTER TABLE datasource ADD CONSTRAINT `fk_datasource_dashboard` FOREIGN KEY (dashboardId) REFERENCES dashboard(id);
ALTER TABLE widget_datasource_map ADD CONSTRAINT `fk_widget_datasource_mapping_widget` FOREIGN KEY (widgetId) REFERENCES widget(id);
ALTER TABLE widget_datasource_map ADD CONSTRAINT `fk_widget_datasource_mapping_datasource` FOREIGN KEY (datasourceId) REFERENCES datasource(id);
ALTER TABLE topology ADD CONSTRAINT `fk_topology_topology_version` FOREIGN KEY (versionId) REFERENCES topology_version(id);
ALTER TABLE topology ADD CONSTRAINT `fk_topology_namespace` FOREIGN KEY (namespaceId) REFERENCES namespace(id);
ALTER TABLE topology_editor_metadata ADD CONSTRAINT `fk_topology_editor_metadata_topology_version` FOREIGN KEY (versionId) REFERENCES topology_version(id);
ALTER TABLE topology_stream ADD CONSTRAINT `fk_topology_stream_topology_version` FOREIGN KEY (versionId) REFERENCES topology_version(id);
ALTER TABLE topology_source ADD CONSTRAINT `fk_topology_source_topology_version` FOREIGN KEY (versionId) REFERENCES topology_version(id);
ALTER TABLE topology_source_stream_map ADD CONSTRAINT `fk_topology_source_stream_mapping_topology_source` FOREIGN KEY (sourceId, versionId) REFERENCES topology_source(id, versionId);
ALTER TABLE topology_source_stream_map ADD CONSTRAINT `fk_topology_source_stream_mapping_topology_stream` FOREIGN KEY (streamId, versionId) REFERENCES topology_stream(id, versionId);
ALTER TABLE topology_sink ADD CONSTRAINT `fk_topology_sink_topology_version` FOREIGN KEY (versionId) REFERENCES topology_version(id);
ALTER TABLE topology_processor ADD CONSTRAINT `fk_topology_processor_topology_version` FOREIGN KEY (versionId) REFERENCES topology_version(id);
ALTER TABLE topology_processor_stream_map ADD CONSTRAINT `fk_topology_processor_stream_mapping_topology_processor` FOREIGN KEY (processorId, versionId) REFERENCES topology_processor(id, versionId);
ALTER TABLE topology_processor_stream_map ADD CONSTRAINT `fk_topology_processor_stream_mapping_topology_stream` FOREIGN KEY (streamId, versionId) REFERENCES topology_stream(id, versionId);
ALTER TABLE topology_edge ADD CONSTRAINT `fk_topology_edge_topology_version` FOREIGN KEY (versionId) REFERENCES topology_version(id);
ALTER TABLE topology_rule ADD CONSTRAINT `fk_topology_rule_topology_version` FOREIGN KEY (versionId) REFERENCES topology_version(id);
ALTER TABLE topology_branchrule ADD CONSTRAINT `fk_topology_branchrule_topology_version` FOREIGN KEY (versionId) REFERENCES topology_version(id);
ALTER TABLE topology_window ADD CONSTRAINT `fk_topology_window_topology_version` FOREIGN KEY (versionId) REFERENCES topology_version(id);
ALTER TABLE service ADD CONSTRAINT `fk_service_cluster` FOREIGN KEY (clusterId) REFERENCES cluster (id);
ALTER TABLE service_configuration ADD CONSTRAINT `fk_service_configuration_service` FOREIGN KEY (serviceId) REFERENCES service (id);
ALTER TABLE component ADD CONSTRAINT `fk_component_service` FOREIGN KEY (serviceId) REFERENCES service (id);
ALTER TABLE component_process ADD CONSTRAINT `fk_component_process_component` FOREIGN KEY (componentId) REFERENCES component (id);
ALTER TABLE role_hierarchy ADD CONSTRAINT `fk_role_hierarchy_role_for_parent` FOREIGN KEY (parentId) REFERENCES role (id);
ALTER TABLE role_hierarchy ADD CONSTRAINT `fk_role_hierarchy_role_for_child` FOREIGN KEY (childId) REFERENCES role (id);
ALTER TABLE user_role ADD CONSTRAINT `fk_user_role_user_entry` FOREIGN KEY (userId) REFERENCES user_entry (id);
ALTER TABLE user_role ADD CONSTRAINT `fk_user_role_role` FOREIGN KEY (roleId) REFERENCES role (id);
ALTER TABLE topology_editor_toolbar ADD CONSTRAINT `fk_topology_editor_toolbar_user_entry` FOREIGN KEY (userId) REFERENCES user_entry(id);
ALTER TABLE topology_test_run_case ADD CONSTRAINT `fk_topology_test_run_case_topology` FOREIGN KEY (topologyId, versionId) REFERENCES topology(id, versionId);
ALTER TABLE topology_test_run_case_source ADD CONSTRAINT `fk_topology_test_run_case_source_topology_test_run_case` FOREIGN KEY (testCaseId) REFERENCES topology_test_run_case(id);
ALTER TABLE topology_test_run_case_source ADD CONSTRAINT `fk_topology_test_run_case_source_topology_source` FOREIGN KEY (sourceId, versionId) REFERENCES topology_source(id, versionId);
ALTER TABLE topology_test_run_case_sink ADD CONSTRAINT `fk_topology_test_run_case_sink_topology_test_run_case` FOREIGN KEY (testCaseId) REFERENCES topology_test_run_case(id);
ALTER TABLE topology_test_run_case_sink ADD CONSTRAINT `fk_topology_test_run_case_sink_topology_sink` FOREIGN KEY (sinkId, versionId) REFERENCES topology_sink(id, versionId);
ALTER TABLE topology_test_run_histories ADD CONSTRAINT `fk_topology_test_run_histories_topology` FOREIGN KEY (topologyId, versionId) REFERENCES topology(id, versionId);

-- ISSUE-910 intentionally exclude adding FK for topology_test_run_case in topology_test_run_histories