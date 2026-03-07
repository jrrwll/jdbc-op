package org.dreamcat.daily.script.base;

import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.daily.script.ability.DataSourceAbility;
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

    String dbTablesFile;
    String dbTablesContent; // json

    @Override
    public void run() throws Exception {
        jdbc.run(this::handle);
    }

    private void handle(Connection connection) throws Exception {
        List<String> databases = dataSource.getDatabases(connection);
        if (ObjectUtil.isEmpty(databases)) {
            log.error("no databases found");
            return;
        }

        for (String database : databases) {
            String targetDatabase = dbTableMappingAbility.getDatabaseMappings(database);
            if (targetDatabase == null) {
                continue;
            }

            List<String> tables = dataSource.getTables(connection, database);
            if (ObjectUtil.isEmpty(tables)) {
                log.error("no tables found on database " + database);
                return;
            }

            for (String table : tables) {
                String targetTable = dbTableMappingAbility.getTableMappings(database, table);
                if (targetTable == null) {
                    continue;
                }

                log.info("starting to sync {}.{} to {}.{}", database, table, targetDatabase, targetTable);
                sync(connection, database, table, targetDatabase, targetTable);
            }
        }
    }
}
