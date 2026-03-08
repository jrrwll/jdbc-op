package org.dreamcat.daily.script.ability;

import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.Quadruple;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.daily.script.base.BaseHandler;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jerry Will
 * @version 2026-03-07
 */
@Slf4j
@ArgParserType(allProperties = true)
public class DbTableMappingsAbility {

    @ArgParserField("dtmf")
    public String dbTableMappingsFile;
    @ArgParserField("dtm")
    public String dbTableMappings; // json content

    transient DataSourceAbility dataSource;
    transient BaseHandler handler;

    public void init(DataSourceAbility dataSource, BaseHandler handler) {
        this.dataSource = dataSource;
        this.handler = handler;
    }

    public List<Quadruple<String, String, String, String>> mappingDbTables(Connection connection) throws Exception {
        List<Quadruple<String, String, String, String>> result = new ArrayList<>();
        List<String> databases = dataSource.getDatabases(connection);
        if (ObjectUtil.isEmpty(databases)) {
            log.warn("no databases found");
            return result;
        }

        for (String database : databases) {
            String targetDatabase = getDatabaseMappings(database);
            if (targetDatabase == null) {
                continue;
            }

            List<String> tables = dataSource.getTables(connection, database);
            if (ObjectUtil.isEmpty(tables)) {
                log.warn("no tables found on database {}", database);
                continue;
            }

            for (String table : tables) {
                String targetTable = getTableMappings(database, table);
                if (targetTable == null) {
                    continue;
                }

                result.add(Quadruple.of(database, table, targetDatabase, targetTable));
            }
        }
        return result;
    }

    private String getDatabaseMappings(String database) {
        return null;
    }

    private String getTableMappings(String database, String table) {
        return null;
    }
}
