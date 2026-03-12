package org.dreamcat.daily.script.ability;

import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.Quadruple;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.daily.script.base.BaseHandler;
import org.dreamcat.daily.script.common.DbTableUtil;
import org.dreamcat.daily.script.model.DbTableMappings;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    transient List<DbTableMappings> dbTableMappingsList;

    transient DataSourceAbility dataSource;
    transient BaseHandler handler;

    public void init(DataSourceAbility dataSource, BaseHandler handler) {
        this.dataSource = dataSource;
        this.handler = handler;

        this.dbTableMappingsList = DbTableMappings.parse(dbTableMappingsFile, dbTableMappings);
    }

    public List<Quadruple<String, String, String, String>> mappingDbTables(Connection connection) throws Exception {
        List<Quadruple<String, String, String, String>> result = new ArrayList<>();

        Set<String> databases = dbTableMappingsList.stream()
                .map(DbTableMappings::getDb)
                .collect(Collectors.toSet());
        if (DbTableUtil.checkExistingDatabases(dataSource, connection, databases)) {
            return result;
        }

        for (DbTableMappings tableMappings : dbTableMappingsList) {
            String db = tableMappings.getDb();
            List<String> tables = new ArrayList<>(tableMappings.getTableMappings().keySet());

            DbTableUtil.checkExistingTables(
                    dataSource, connection, db, db, tables);
            for (String table : tables) {
                String targetTable = tableMappings.getTableMappings().get(table);
                String targetDb = tableMappings.getTargetDb();
                if (targetDb == null) {
                    targetDb = db;
                }
                result.add(Quadruple.of(db, table, targetDb, targetTable));
            }
        }
        return result;
    }
}
