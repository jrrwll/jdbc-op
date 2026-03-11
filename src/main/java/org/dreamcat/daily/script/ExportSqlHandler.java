package org.dreamcat.daily.script;

import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.sql.JdbcColumnDef;
import org.dreamcat.daily.script.ability.OutputAbility;
import org.dreamcat.daily.script.base.BaseExportHandler;

import java.sql.Connection;
import java.util.Collections;
import java.util.Map;

/**
 * @author Jerry Will
 * @version 2023-08-14
 */
@Slf4j
@ArgParserType(command = "export-sql")
public class ExportSqlHandler extends BaseExportHandler {

    boolean columnQuota;

    OutputAbility outputAbility;

    @Override
    protected void exportTable(Connection connection, String database, String table) throws Exception {
        Map<String, JdbcColumnDef> columnMap = dataSource.getColumnMap(connection, database, table);
        dataSource.getRows(connection, database, table, batchSize, rows -> {
            String sql = dataSource.getInsertIntoSql(rows, columnMap,
                    database, table, columnQuota);
            try {
                outputAbility.run(Collections.singletonList(sql), verbose);
            } catch (Exception e) {
                log.error("failed to export {}.{}", database, table, e);
            }
        });
    }
}
