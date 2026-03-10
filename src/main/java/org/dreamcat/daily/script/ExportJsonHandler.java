package org.dreamcat.daily.script;

import lombok.SneakyThrows;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.text.InterpolationUtil;
import org.dreamcat.daily.script.base.BaseExportHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * @author Jerry Will
 * @version 2026-03-11
 */
@ArgParserType(command = "export-json")
public class ExportJsonHandler extends BaseExportHandler {

    @ArgParserField("o")
    private String outputFile = "$database/$table.csv";

    @Override
    protected void exportTable(Connection connection, String database, String table) throws Exception {
        dataSource.getRows(connection, database, table, batchSize, rows -> {
            exportRows(database, table, rows);
        });
    }

    @SneakyThrows
    protected void exportRows(String database, String table, List<Map<String, Object>> rows) {
        String outputName = InterpolationUtil.format(outputFile,
                "database", database, "db", database,
                "table", table, "tb", table);
        File outputFile = new File(outputName);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            for (Map<String, Object> row : rows) {
                writer.write(JsonUtil.toJson(row));
                writer.newLine();
            }
        }
    }
}
