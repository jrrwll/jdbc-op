package org.dreamcat.daily.script.ability;

import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.Pair;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.daily.script.base.BaseHandler;
import org.dreamcat.daily.script.common.DbTableUtil;
import org.dreamcat.daily.script.model.DbTables;

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
public class DbTablesAbility {

    @ArgParserField
    public List<String> tables; // no database specified
    @ArgParserField("dtf")
    public String dbTablesFile;
    @ArgParserField("dt")
    public String dbTables; // json content

    transient List<DbTables> dbTablesList;

    transient DataSourceAbility dataSource;
    transient BaseHandler handler;

    public void init(DataSourceAbility dataSource, BaseHandler handler) {
        this.dataSource = dataSource;
        this.handler = handler;

        if (tables == null) {
            this.dbTablesList = DbTables.parse(dbTablesFile, dbTables);
        }
    }

    public List<Pair<String, String>> getDbTables(Connection connection) throws Exception {
        if (tables != null) {
            return DbTableUtil.checkExistingTables(
                    dataSource, connection,
                    connection.getSchema(), null, tables);
        }

        List<Pair<String, String>> result = new ArrayList<>();

        Set<String> databases = dbTablesList.stream()
                .map(DbTables::getDb)
                .collect(Collectors.toSet());
        if (DbTableUtil.checkExistingDatabases(dataSource, connection, databases)) {
            return result;
        }

        for (DbTables database : dbTablesList) {
            String db = database.getDb();
            result.addAll(DbTableUtil.checkExistingTables(
                    dataSource, connection,
                    db, db, database.getTables()));
        }
        return result;
    }
}
