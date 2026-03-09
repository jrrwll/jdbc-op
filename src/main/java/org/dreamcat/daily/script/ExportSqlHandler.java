package org.dreamcat.daily.script;

import lombok.SneakyThrows;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.sql.JdbcColumnDef;
import org.dreamcat.daily.script.ability.OutputAbility;
import org.dreamcat.daily.script.base.BaseExportHandler;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2023-08-14
 */
@ArgParserType(command = "export-sql")
public class ExportSqlHandler extends BaseExportHandler {

    boolean columnQuota;

    OutputAbility outputAbility;

    @Override
    protected void exportTable(Connection connection, String database, String table) throws Exception {
        Map<String, JdbcColumnDef> columnMap = dataSource.getColumnMap(connection, database, table);
        dataSource.getRows(connection, database, table, batchSize, rows -> {
            exportRows(database, table, rows, columnMap);
        });
    }

    @SneakyThrows
    protected void exportRows(String database, String table, List<Map<String, Object>> rows,
            Map<String, JdbcColumnDef> columnMap) {
        List<String> columnNames = new ArrayList<>(rows.get(0).keySet());
        List<String> typeNames = columnNames.stream()
                .map(columnName -> columnMap.get(columnName).getType().toLowerCase())
                .collect(Collectors.toList());
        List<List<Object>> rowList = rows.stream()
                .map(map -> new ArrayList<>(map.values()))
                .collect(Collectors.toList());
        String sql = dataSource.getInsertIntoSql(rowList, columnNames, typeNames,
                database, table, columnQuota);

        outputAbility.run(Collections.singletonList(sql));
    }

}
