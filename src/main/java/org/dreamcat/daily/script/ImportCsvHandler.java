package org.dreamcat.daily.script;

import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.io.CsvUtil;
import org.dreamcat.common.text.TextValueType;
import org.dreamcat.common.util.ListUtil;
import org.dreamcat.daily.script.base.BaseImportHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2023-06-17
 */
@Slf4j
@ArgParserType(command = "import-csv")
public class ImportCsvHandler extends BaseImportHandler {

    @ArgParserField("f")
    private String file;
    private boolean tsv;
    private boolean emptyStringAsNull;

    @ArgParserField(position = 1)
    private String tableName;

    @Override
    public void run() throws Exception {
        init();

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

        log.info("start to import {}, total {}", header, rows.size());
        long cost = System.currentTimeMillis();
        importTable(header, rows);
        cost = System.currentTimeMillis() - cost;
        log.info("finish to import {}, cost {}ms", header, cost);
    }

    private void importTable(List<String> columnNames, List<List<String>> rows) throws Exception {
        List<List<Object>> list = rows.stream()
                .filter(row -> row.size() == columnNames.size())
                .map(this::mapToRow)
                .collect(Collectors.toList());
        List<String> columnTypes = dataSourceAbility.detectColumnTypes(list.get(0));

        List<String> sqlList = new ArrayList<>();
        List<List<List<Object>>> partition = ListUtil.partition(list, batchSize);
        for (List<List<Object>> rowList : partition) {
            String insertIntoSql = dataSourceAbility.getInsertIntoSql(rowList, columnNames, columnTypes,
                    database, tableName, columnQuota);
            sqlList.add(insertIntoSql);
        }

        if (!yes) {
            outputAbility.run(sqlList);
        } else {
            jdbcAbility.executeSql(sqlList, verbose, abort);
        }
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
