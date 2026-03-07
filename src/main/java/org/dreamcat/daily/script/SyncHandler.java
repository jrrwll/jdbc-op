package org.dreamcat.daily.script;

import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.Quadruple;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.daily.script.ability.DataSourceAbility;
import org.dreamcat.daily.script.ability.DbTableMappingsAbility;
import org.dreamcat.daily.script.ability.JdbcAbility;
import org.dreamcat.daily.script.base.BaseHandler;

import java.sql.Connection;
import java.util.List;

/**
 * @author Jerry Will
 * @version 2023-08-14
 */
@Slf4j
@ArgParserType(command = "sync")
public class SyncHandler extends BaseHandler {

    @ArgParserField(nested = true, nestedPrefix = "from")
    DataSourceAbility dataSourceFrom;
    @ArgParserField(nested = true, nestedPrefix = "from")
    JdbcAbility jdbcFrom;
    @ArgParserField(nested = true, nestedPrefix = "to")
    JdbcAbility jdbcTo;
    @ArgParserField(nested = true, nestedPrefix = "to")
    DataSourceAbility dataSourceTo;

    @ArgParserField(nested = true)
    DbTableMappingsAbility dbTableMappingsAbility;

    @Override
    public void run() throws Exception {
        dbTableMappingsAbility.init(dataSourceFrom, this);

        jdbcFrom.run(this::handle);
    }

    private void handle(Connection connection) throws Exception {
        List<Quadruple<String, String, String, String>> dbTableMappings =
                dbTableMappingsAbility.mappingDbTables(connection);
        for (Quadruple<String, String, String, String> quadruple : dbTableMappings) {
            String database = quadruple.first();
            String table = quadruple.second();
            String targetDatabase = quadruple.third();
            String targetTable = quadruple.third();
            log.info("starting to sync {}.{} to {}.{}", database, table, targetDatabase, targetTable);
            long cost = System.currentTimeMillis();
            int rowCount = doSync(connection, database, table, targetDatabase, targetTable);
            cost = System.currentTimeMillis() - cost;
            log.info("finished to sync {}.{} to {}.{}, rows {}, cost {}ms", database, table, targetDatabase, targetTable, rowCount, cost);
        }
    }

    private int doSync(Connection connection, String database, String table,
            String targetDatabase, String targetTable) {
        return 0 ;
    }
}
