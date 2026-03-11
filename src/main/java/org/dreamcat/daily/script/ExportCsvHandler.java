package org.dreamcat.daily.script;

import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.io.CsvUtil;
import org.dreamcat.daily.script.base.BaseExportHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Jerry Will
 * @version 2023-08-14
 */
@ArgParserType(command = "export-csv")
public class ExportCsvHandler extends BaseExportHandler.Appendable {

    @Override
    protected String getSuffix() {
        return "csv";
    }

    @Override
    protected void exportRows(String database, String table,
            List<Map<String, Object>> rows, File outputFile) throws Exception {
        CsvUtil.write(rows.stream().map(this::mapToRow).iterator(), outputFile, true);
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
