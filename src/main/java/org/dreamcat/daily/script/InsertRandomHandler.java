package org.dreamcat.daily.script;

import static org.dreamcat.common.util.RandomUtil.randi;

import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.sql.JdbcColumnDef;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.daily.script.ability.DataSourceRandomGenAbility;
import org.dreamcat.daily.script.ability.JdbcAbility;
import org.dreamcat.daily.script.ability.OutputAbility;
import org.dreamcat.daily.script.base.BaseHandler;
import org.dreamcat.daily.script.common.AbortException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jerry Will
 * @version 2023-03-22
 */
@Slf4j
@ArgParserType(command = "insert-random")
public class InsertRandomHandler extends BaseHandler {

    @ArgParserField(position = 1)
    private String tableName;

    @ArgParserField(nested = true)
    DataSourceRandomGenAbility dataSourceAbility;
    @ArgParserField(nested = true)
    JdbcAbility jdbcAbility;
    @ArgParserField(nested = true)
    OutputAbility outputAbility;

    @ArgParserField
    boolean columnQuota;
    @ArgParserField({"n"})
    int batchSize = 1;
    @ArgParserField({"tc"})
    int totalCount = randi(1, 76);
    @ArgParserField(firstChar = true)
    boolean yes;

    transient String database;

    @Override
    public void init() throws Exception {
        if (ObjectUtil.isBlank(tableName)) {
            throw new AbortException("table name is required");
        }

        dataSourceAbility.init();

        String[] dbTable = tableName.split(",", 2);
        if (dbTable.length == 2) {
            database = dbTable[0];
            tableName = dbTable[1];
        }
    }

    @Override
    public void run() throws Exception {
        List<String> columnNames = new ArrayList<>();
        List<String> columnTypes = new ArrayList<>();
        jdbcAbility.run(connection -> {
            List<JdbcColumnDef> columns = dataSourceAbility.getColumns(
                    connection, database, tableName);
            for (JdbcColumnDef column : columns) {
                columnNames.add(column.getName());
                columnTypes.add(column.getType());
            }
        }, verbose);

        // insert sql
        List<String> sqlList = new ArrayList<>();
        int rowNum = totalCount;
        while (rowNum > batchSize) {
            rowNum -= batchSize;

            List<List<Object>> rows = dataSourceAbility.generateValues(
                    columnTypes, batchSize, null);
            String insertIntoSql = dataSourceAbility.getInsertIntoSql(
                    rows, columnNames, columnTypes,
                    database, tableName, columnQuota);
            sqlList.add(insertIntoSql);
        }

        if (!yes) {
            outputAbility.run(sqlList, verbose);
        } else {
            jdbcAbility.executeSql(sqlList, verbose, abort);
        }
    }

}
