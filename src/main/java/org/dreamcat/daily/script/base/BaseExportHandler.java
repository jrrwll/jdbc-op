package org.dreamcat.daily.script.base;

import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.Pair;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.daily.script.ability.DataSourceAbility;
import org.dreamcat.daily.script.ability.DbTablesAbility;
import org.dreamcat.daily.script.ability.JdbcAbility;

import java.sql.Connection;
import java.util.List;

/**
 * @author Jerry Will
 * @version 2023-08-14
 */
@Slf4j
public abstract class BaseExportHandler extends BaseHandler {

    @ArgParserField(nested = true)
    DataSourceAbility dataSource;
    @ArgParserField(nested = true)
    JdbcAbility jdbc;

    @ArgParserField(nested = true)
    DbTablesAbility dbTablesAbility;;

    @Override
    public void run() throws Exception {
        jdbc.run(this::handle);
    }

    private void handle(Connection connection) throws Exception {
        List<Pair<String, String>> dbTables = dbTablesAbility.getDbTables(connection);
        for (Pair<String, String> dbTable : dbTables) {
            String database = dbTable.getFirst();
            String table = dbTable.getSecond();
            doExport(connection, database, table);
        }
    }

    private void doExport(Connection connection, String database, String table) {

    }
}
