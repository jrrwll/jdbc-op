package org.dreamcat.daily.script;

import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.Triple;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.io.CsvUtil;
import org.dreamcat.common.text.TextValueType;
import org.dreamcat.daily.script.base.BaseImportHandler.SingleTable;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2023-06-17
 */
@Slf4j
@ArgParserType(command = "import-csv")
public class ImportCsvHandler extends SingleTable {

    private boolean tsv;
    private boolean emptyStringAsNull;

    @Override
    protected Triple<List<List<Object>>, List<String>, List<String>> readAllRows() throws Exception {
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

        List<String> columnNames = rows.get(0);
        List<List<Object>> rowList = rows.subList(1, rows.size()).stream()
                .filter(row -> row.size() == columnNames.size())
                .map(this::mapToRow)
                .collect(Collectors.toList());
        List<String> columnTypes = dataSourceAbility.detectColumnTypes(rowList.get(0));
        return Triple.of(rowList, columnNames, columnTypes);
    }

    private List<Object> mapToRow(List<String> row) {
        return row.stream().map(value -> {
            if (emptyStringAsNull && value.isEmpty()) {
                return null;
            }
            return TextValueType.parse(value);
        }).collect(Collectors.toList());
    }
}
