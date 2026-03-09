package org.dreamcat.daily.script;

import lombok.SneakyThrows;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.sql.JdbcColumnDef;
import org.dreamcat.daily.script.ability.OutputAbility;
import org.dreamcat.daily.script.base.BaseExportHandler;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        String sql = dataSource.getInsertIntoSql(rows, columnMap, database, table, columnQuota);

        outputAbility.run(Collections.singletonList(sql));
    }

}
