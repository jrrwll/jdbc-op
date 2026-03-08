package org.dreamcat.daily.script.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * @author Jerry Will
 * @version 2023-05-29
 */
@Getter
@Setter
public class DataSourceInfos {

    private Map<String, List<String>> dataSourceAlias;
    // template -> dataSourceType -> columnType
    private Map<String, Map<String, List<String>>> castLiteral;

    private Map<String, List<String>> databaseSchemaSql;
    private Map<String, List<String>> tableSchemaSql;
    private Map<String, List<String>> columnSchemaSql;
    private List<String> doubleQuota;
}
