package org.dreamcat.daily.script;

import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.Pair;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.daily.script.base.BaseImportHandler;
import org.dreamcat.daily.script.common.AbortException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2026-03-11
 */
@Slf4j
@SuppressWarnings({"unchecked", "rawtypes"})
@ArgParserType(command = "import-json")
public class ImportJsonHandler extends BaseImportHandler.SingleTable {

    @ArgParserField("jsonl")
    boolean jsonLine;
    @ArgParserField("json")
    boolean jsonNormal;

    @Override
    protected Pair<List<String>, List<List<Object>>> readAllRows() throws Exception {
        if (file == null) {
            throw new AbortException("require file: -f|--file <file>");
        }

        List<Map<String, Object>> rows;
        if (jsonLine) {
            rows = readJsonLine();
        } else if (jsonNormal) {
            rows = readJson();
        } else {
            String suffix = FileUtil.suffix(file).toLowerCase();
            if (suffix.equals("jsonl")) {
                rows = readJsonLine();
            } else if (suffix.equals("json")) {
                rows = readJson();
            } else {
                throw new AbortException("must specify file format by pass: --jsonl or --json");
            }
        }

        List<String> columnNames = new ArrayList<>(rows.get(0).keySet());
        List<List<Object>> rowList = rows.stream()
                .map(row -> (List<Object>) new ArrayList(row.values()))
                .collect(Collectors.toList());
        log.info("json parsed, columnNames={}, total {} columns and {} rows",
                columnNames, columnNames.size(), rowList.size());
        return Pair.of(columnNames, rowList);
    }

    private List<Map<String, Object>> readJsonLine() throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while((line = reader.readLine()) != null) {
                Map<String, Object> row = JsonUtil.fromJsonObject(line);
                rows.add(row);
            }
        }
        return rows;
    }

    private List<Map<String, Object>> readJson() {
        return (List<Map<String, Object>>) (List) JsonUtil.fromJsonArray(
                new File(file), Map.class);
    }
}
