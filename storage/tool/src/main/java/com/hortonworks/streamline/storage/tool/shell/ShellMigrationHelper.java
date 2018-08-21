/**
 * Copyright 2017 Hortonworks.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *   http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package com.hortonworks.streamline.storage.tool.shell;

import com.hortonworks.streamline.storage.tool.shell.exception.ShellMigrationException;
import org.flywaydb.core.Flyway;

import java.sql.SQLException;

import static org.flywaydb.core.internal.info.MigrationInfoDumper.dumpToAsciiTable;

public class ShellMigrationHelper {
    private Flyway flyway;

    public ShellMigrationHelper(Flyway flyway) {
        this.flyway = flyway;
    }

    private void migrate() throws SQLException {
        flyway.migrate();
    }

    private void info() {
        System.out.println(dumpToAsciiTable(flyway.info().all()));
    }

    private void validate() {
        flyway.validate();
    }

    private void repair() {
        flyway.repair();
    }

    public void execute(ShellMigrationOption schemaMigrationOption) throws SQLException {
        switch (schemaMigrationOption) {
            case MIGRATE:
                migrate();
                break;
            case INFO:
                info();
                break;
            case VALIDATE:
                validate();
                break;
            case REPAIR:
                repair();
                break;
            default:
                throw new ShellMigrationException("ShellMigrationHelper unable to execute the option : " + schemaMigrationOption.toString());
        }
    }
}
