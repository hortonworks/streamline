package org.apache.streamline.streams.catalog.service.metadata.common;

import org.apache.hadoop.hbase.TableName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wrapper used to show proper JSON formatting
 */
public class Tables {
    private List<String> tables;

    public Tables(List<String> tables) {
        this.tables = tables;
    }

    public static Tables newInstance(TableName[] tableNames) {
        List<String> fqTableNames = Collections.emptyList();
        if (tableNames != null) {
            fqTableNames = new ArrayList<>(tableNames.length);
            for (TableName tableName : tableNames) {
                fqTableNames.add(tableName.getNameWithNamespaceInclAsString());
            }
        }
        return new Tables(fqTableNames);
    }

    public static Tables newInstance(List<String> tables) {
        return tables == null ? new Tables(Collections.<String>emptyList()) : new Tables(tables);
    }

    public List<String> getTables() {
        return tables;
    }
}
