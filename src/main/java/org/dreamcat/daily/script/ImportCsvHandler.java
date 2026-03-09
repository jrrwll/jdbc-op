package org.dreamcat.daily.script;

import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.io.CsvUtil;
import org.dreamcat.daily.script.base.BaseImportHandler;

import java.io.File;
import java.util.List;

/**
 * @author Jerry Will
 * @version 2023-06-17
 */
@ArgParserType(command = "import-csv")
public class ImportCsvHandler extends BaseImportHandler {

    @ArgParserField("f")
    private String file;
    private boolean tsv;
    private boolean emptyStringAsNull;

    @ArgParserField(position = 1)
    private String tableName;

    public void init() {

    }

    @Override
    public void run() throws Exception {
        init();

        handle();
    }

    private void handle() throws Exception {
        List<List<String>> rows;
        if (tsv) {
            rows = CsvUtil.readTsv(new File(file));
        } else {
            rows = CsvUtil.read(new File(file));
        }

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("csv file is empty");
        }
        if (rows.size() < 2) {
            throw new IllegalArgumentException("require at least two rows in your data file");
        }
        int headerWidth = rows.get(0).size();
        if (rows.stream().anyMatch(row -> row.size() < headerWidth)) {
            throw new IllegalArgumentException("require same column width in your data file");
        }

        List<String> header = rows.get(0);
        rows = rows.subList(1, rows.size());
        importTable(header, rows);
    }

    private void importTable(List<String> header, List<List<String>> rows) {

    }
}
