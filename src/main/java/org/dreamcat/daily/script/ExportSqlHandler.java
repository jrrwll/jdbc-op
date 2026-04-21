package org.dreamcat.daily.script;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.sql.JdbcColumnDef;
import org.dreamcat.daily.script.ability.DataSourceTextTypeAbility;
import org.dreamcat.daily.script.base.BaseExportHandler;

import java.io.File;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2023-08-14
 */
@Slf4j
@ArgParserType(command = "export-sql")
@SuppressWarnings({"unchecked", "rawtypes"})
public class ExportSqlHandler extends BaseExportHandler {

    @Getter
    @ArgParserField(nested = true)
    DataSourceTextTypeAbility dataSourceAbility;

    @ArgParserField
    String outputTable = "table_name";
    @ArgParserField
    boolean columnQuota;

    @Override
    public void exportTable(Connection connection, String database, String table) throws Exception {
        File file  = getTableOutputFile(database, table, "sql");

        Map<String, JdbcColumnDef> columnMap = dataSourceAbility.getColumnMap(connection, database, table);
        dataSourceAbility.getRows(connection, database, table, batchSize, rows -> {
            String sql = dataSourceAbility.getInsertIntoSql(rows, columnMap,
                    database, table, columnQuota);
            try {
                FileUtil.write(file, sql, true);
            } catch (Exception e) {
                log.error("failed to export {}.{}", database, table, e);
            }
        });
    }

    @Override
    protected void exportSql(Connection connection, String sql) throws Exception {
        File file = getSqlOutputFile("sql");
        String database, table;
        if (outputTable.contains(".")) {
            String[] split = outputTable.split("\\.");
            database = split[0];
            table = split[1];
        } else {
            database = null;
            table = outputTable;
        }

        List<String> columnNames = new ArrayList<>();
        List<String> columnTypes = new ArrayList<>();
        dataSourceAbility.getRows(connection, sql, batchSize, rows -> {
            List<List<Object>> rowList = rows.stream()
                    .map(row -> (List<Object>) new ArrayList(row.values()))
                    .collect(Collectors.toList());
            if (columnNames.isEmpty()) {
                columnNames.addAll(rows.get(0).keySet());
                columnTypes.addAll(dataSourceAbility.detectColumnTypes(rowList.get(0)));
            }

            String outputSql = dataSourceAbility.getInsertIntoSql(rowList, columnNames,
                    columnTypes, database, table, columnQuota);
            try {
                FileUtil.write(file, outputSql, true);
            } catch (Exception e) {
                log.error("failed to export: {}", sql, e);
            }
        });
    }
}
