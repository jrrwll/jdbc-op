package org.dreamcat.daily.script;

import lombok.SneakyThrows;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.io.CsvUtil;
import org.dreamcat.common.text.InterpolationUtil;
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
@ArgParserType(command = "export-csv")
public class ExportCsvHandler extends BaseExportHandler {

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

        List<List<String>> list = rows.stream().map(this::mapToRow)
                .collect(Collectors.toList());
        CsvUtil.write(list, outputFile, true);
    }

    private List<String> mapToRow(Map<String, Object> map) {
        List<String> row = new ArrayList<>(map.size());
        for (Object value : map.values()) {
            if (value != null) {
                row.add(value.toString());
            } else {
                row.add("");
            }
        }
        return row;
    }
}
