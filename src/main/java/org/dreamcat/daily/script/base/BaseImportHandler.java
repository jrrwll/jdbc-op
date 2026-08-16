package org.dreamcat.daily.script.base;

import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.Pair;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.util.ListUtil;
import org.dreamcat.daily.script.ability.DataSourceTextTypeAbility;
import org.dreamcat.daily.script.ability.DdlSqlAbility;
import org.dreamcat.daily.script.ability.JdbcAbility;
import org.dreamcat.daily.script.ability.OutputAbility;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jerry Will
 * @version 2023-06-27
 */
@Slf4j
public abstract class BaseImportHandler extends BaseHandler {

    @ArgParserField(nested = true)
    protected JdbcAbility jdbcAbility;
    @ArgParserField(nested = true)
    protected DataSourceTextTypeAbility dataSourceAbility;
    @ArgParserField(nested = true)
    protected OutputAbility outputAbility;

    @ArgParserField
    protected String database;
    @ArgParserField({"n"})
    protected int batchSize = 1000;

    @ArgParserField
    protected boolean compact;
    @ArgParserField
    protected boolean columnQuota;
    @ArgParserField
    protected boolean yes;

    @ArgParserField({"create"})
    protected boolean createTableIfNotExists;
    @ArgParserField(nested = true)
    protected DdlSqlAbility ddlSqlAbility;

    public void init() throws Exception {
        dataSourceAbility.init();
        ddlSqlAbility.init(dataSourceAbility);
    }

    protected void importTable(
            String tableName, List<String> columnNames,
            List<List<Object>> rows) throws Exception {
        List<String> columnTypes = dataSourceAbility.detectColumnTypes(rows.get(0));

        List<String> sqlList = new ArrayList<>();
        if (createTableIfNotExists) {
            jdbcAbility.run(connection -> {
                List<String> ddlSql = ddlSqlAbility.checkAndGetCreateSql(
                        connection, database, tableName,
                        columnNames, columnTypes, compact, columnQuota);
                sqlList.addAll(ddlSql);
            }, verbose);
        }

        List<List<List<Object>>> partition = ListUtil.partition(rows, batchSize);
        for (List<List<Object>> rowList : partition) {
            String insertIntoSql = dataSourceAbility.getInsertIntoSql(
                    rowList, columnNames, columnTypes,
                    database, tableName, columnQuota);
            sqlList.add(insertIntoSql);
        }

        if (!yes) {
            outputAbility.run(sqlList, verbose);
        } else {
            jdbcAbility.executeSql(sqlList, verbose, abort);
        }
    }

    public static abstract class SingleTable extends BaseImportHandler {

        @ArgParserField("f")
        protected String file;

        @ArgParserField(position = 1)
        private String tableName;

        protected abstract Pair<List<String>, List<List<Object>>> readAllRows() throws Exception;

        @Override
        public void run() throws Exception {
            Pair<List<String>, List<List<Object>>> pair = readAllRows();
            log.info("start to import, total {}", pair.first().size());
            long cost = System.currentTimeMillis();
            List<String> columnNames = pair.first();
            List<List<Object>> rows = pair.second();
            importTable(tableName, columnNames, rows);
            cost = System.currentTimeMillis() - cost;
            log.info("finish to import, cost {}ms", cost);
        }
    }
}
