package org.dreamcat.daily.script;

import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.excel.build.ExcelBuilder;
import org.dreamcat.common.excel.style.ExcelStyle;
import org.dreamcat.daily.script.base.BaseExportHandler;

import java.io.File;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Jerry Will
 * @version 2026-03-05
 */
@ArgParserType(command = "export-excel")
public class ExportExcelHandler extends BaseExportHandler {

    @Override
    protected void exportTable(Connection connection, String database, String table) throws Exception {
        String outputName = formatOutputName(database, table, "xlsx");
        File outputFile = new File(outputName);

        List<String> headers = new ArrayList<>();
        // excel rows limit is 2^20
        List<List<Object>> body = new ArrayList<>(1024);

        dataSource.getRows(connection, database, table, batchSize, rows -> {
            if (headers.isEmpty()) {
                headers.addAll(rows.get(0).keySet());
            }
            for (Map<String, Object> row : rows) {
                body.add(new ArrayList<>(row.values()));
            }
        });

        ExcelBuilder.build().addSheet(sheet -> {
            sheet.headerStyle(new ExcelStyle()
                            .fontBold()
                            .fontHeight(14))
                    .header(headers)
                    .body(body);
        }).writeTo(outputFile);
    }
}
