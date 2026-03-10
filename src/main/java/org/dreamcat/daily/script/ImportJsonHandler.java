package org.dreamcat.daily.script;

import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.util.ListUtil;
import org.dreamcat.daily.script.base.BaseImportHandler;
import org.dreamcat.daily.script.common.AbortException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2026-03-11
 */
@Slf4j
@SuppressWarnings({"unchecked", "rawtypes"})
@ArgParserType(command = "import-json")
public class ImportJsonHandler extends BaseImportHandler {

    @ArgParserField("f")
    String file;

    @ArgParserField("jsonl")
    boolean jsonLine;
    @ArgParserField("json")
    boolean jsonNormal;

    @ArgParserField(position = 1)
    private String tableName;

    @Override
    public void run() throws Exception {
        init();

        if (file == null) {
            throw new AbortException("require file: -f|--file <file>");
        }
        if (jsonLine) {
            handleJsonLine();
        } else if (jsonNormal) {
            handleJson();
        } else {
            String suffix = FileUtil.suffix(file).toLowerCase();
            if (suffix.equals("jsonl")) {
                handleJsonLine();
            } else if (suffix.equals("json")) {
                handleJson();
            } else {
                throw new AbortException("must specify file format by pass: --jsonl or --json");
            }
        }
    }

    private void handleJsonLine() throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            Map<String, Object> row = JsonUtil.fromJsonObject(reader.readLine());
            rows.add(row);
        }

        log.info("start to import, total {}", rows.size());
        long cost = System.currentTimeMillis();
        handleAllRows(rows);
        cost = System.currentTimeMillis() - cost;
        log.info("finish to import, cost {}ms", cost);
    }

    private void handleJson() throws Exception {
        List<Map<String, Object>> rows = (List) JsonUtil.fromJsonArray(
                new File(file), Map.class);

        log.info("start to import, total {}", rows.size());
        long cost = System.currentTimeMillis();
        handleAllRows(rows);
        cost = System.currentTimeMillis() - cost;
        log.info("finish to import, cost {}ms", cost);
    }

    private void handleAllRows(List<Map<String, Object>> rows) throws Exception {
        if (rows.isEmpty()) {
            log.warn("json file is empty");
            return;
        }

        List<List<Object>> list = rows.stream()
                .map(row -> (List<Object>) new ArrayList(row.values()))
                .collect(Collectors.toList());

        List<String> columnNames = new ArrayList<>(rows.get(0).keySet());
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
}
