package org.dreamcat.daily.script.base;

import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.Triple;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.util.ListUtil;
import org.dreamcat.daily.script.ability.DataSourceAbility;
import org.dreamcat.daily.script.ability.JdbcAbility;
import org.dreamcat.daily.script.ability.OutputAbility;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jerry Will
 * @version 2023-06-27
 */
@Slf4j
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class BaseImportHandler extends BaseHandler {

    @ArgParserField(nested = true)
    protected JdbcAbility jdbcAbility;
    @ArgParserField(nested = true)
    protected DataSourceAbility dataSourceAbility;
    @ArgParserField(nested = true)
    protected OutputAbility outputAbility;

    @ArgParserField
    protected String database;
    @ArgParserField({"n"})
    protected int batchSize = 1000;

    @ArgParserField
    protected boolean columnQuota;
    @ArgParserField
    protected boolean yes;

    public void init() throws Exception {
        dataSourceAbility.init();
    }

    public static abstract class SingleTable extends BaseImportHandler {

        @ArgParserField("f")
        protected String file;

        @ArgParserField(position = 1)
        private String tableName;

        @Override
        public void run() throws Exception {
            init();

            Triple<List<List<Object>>, List<String>, List<String>> triple = readAllRows();
            log.info("start to import, total {}", triple.first().size());
            long cost = System.currentTimeMillis();
            handleAllRows(triple.first(), triple.second(), triple.third());
            cost = System.currentTimeMillis() - cost;
            log.info("finish to import, cost {}ms", cost);
        }

        protected abstract Triple<List<List<Object>>, List<String>, List<String>> readAllRows() throws Exception;

        private void handleAllRows(List<List<Object>> rows,
                List<String> columnNames, List<String> columnTypes) throws Exception {
            if (rows.isEmpty()) {
                log.warn("input file is empty");
                return;
            }

            List<String> sqlList = new ArrayList<>();
            List<List<List<Object>>> partition = ListUtil.partition(rows, batchSize);
            for (List<List<Object>> rowList : partition) {
                String insertIntoSql = dataSourceAbility.getInsertIntoSql(rowList, columnNames, columnTypes,
                        database, tableName, columnQuota);
                sqlList.add(insertIntoSql);
            }

            if (!yes) {
                outputAbility.run(sqlList);
            } else {
                jdbcAbility.executeSql(sqlList, verbose, abort);
            }
        }
    }
}
