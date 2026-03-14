package org.dreamcat.daily.script;

import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.Pair;
import org.dreamcat.common.Quadruple;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.sql.JdbcColumnDef;
import org.dreamcat.common.util.ExceptionUtil;
import org.dreamcat.common.util.MapUtil;
import org.dreamcat.daily.script.ability.DataSourceAbility;
import org.dreamcat.daily.script.ability.DbTableMappingsAbility;
import org.dreamcat.daily.script.ability.JdbcAbility;
import org.dreamcat.daily.script.base.BaseHandler;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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

    @ArgParserField({"n"})
    private int batchSize = 1000;

    @Override
    public void run() throws Exception {
        dbTableMappingsAbility.init(dataSourceFrom, this);

        handle();
    }

    private void handle() throws Exception {
        List<Quadruple<String, String, String, String>> dbTableMappings = new ArrayList<>();
        jdbcFrom.run(connection -> {
            dbTableMappings.addAll(dbTableMappingsAbility.mappingDbTables(connection));
        }, verbose);
        if (dbTableMappings.isEmpty()) return;

        for (Quadruple<String, String, String, String> quadruple : dbTableMappings) {
            String database = quadruple.first();
            String table = quadruple.second();
            String targetDatabase = quadruple.third();
            String targetTable = quadruple.third();

            AtomicBoolean quit = new AtomicBoolean();
            jdbcFrom.run(connection -> {
                jdbcTo.run(targetConnection -> {
                    boolean ok = handleOne(connection, targetConnection, database, table, targetDatabase, targetTable);
                    if (!ok) {
                        quit.set(true);
                    }
                }, verbose);
            }, verbose);
            if (quit.get()) {
                break;
            }
        }
    }

    private boolean handleOne(Connection connection, Connection targetConnection,
            String database, String table, String targetDatabase, String targetTable) throws Exception {
        log.info("starting to sync {}.{} to {}.{}", database, table, targetDatabase, targetTable);
        long cost = System.currentTimeMillis();
        int rowCount;
        try {
            rowCount = doHandleOne(connection, targetConnection, database, table, targetDatabase, targetTable);
        } catch (Exception e) {
            log.error("failed to sync {}.{} to {}.{}: {}",
                    database, table, targetDatabase, targetTable,
                    ExceptionUtil.getRootCauseMessage(e));
            return !abort;
        }
        cost = System.currentTimeMillis() - cost;
        log.info("success to sync {}.{} to {}.{}, rows {}, cost {}ms", database, table, targetDatabase, targetTable, rowCount, cost);
        return true;
    }

    private int doHandleOne(Connection connection, Connection targetConnection,
            String database, String table, String targetDatabase, String targetTable) throws Exception {
        List<JdbcColumnDef> columns = dataSourceFrom.getColumns(connection, database, table);
        if (verbose) {
            log.info("{}.{} columns: {}", database, table, columns.stream()
                    .map(c -> Pair.of(c.getName(), c.getType()))
                    .collect(Collectors.toList()));
        }
        Map<String, JdbcColumnDef> columnMap = MapUtil.toMap(columns, JdbcColumnDef::getName);

        dataSourceFrom.getRows(connection, database, table, batchSize, rows -> {
            doHandleRows(targetDatabase, targetTable, rows, columnMap, targetConnection);
        });
        return 0 ;
    }

    private void doHandleRows(String targetDatabase, String targetTable,
            List<Map<String, Object>> rows, Map<String, JdbcColumnDef> columnMap,
            Connection targetConnection) {

    }
}
