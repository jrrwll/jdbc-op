package org.dreamcat.daily.script.ability;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.excel.build.ExcelBuilder;
import org.dreamcat.common.excel.style.ExcelStyle;
import org.dreamcat.common.function.IBiConsumer;
import org.dreamcat.common.io.CsvUtil;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.daily.script.common.AbortException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2026-03-11
 */
@Slf4j
public class ExportAbility {

    @ArgParserField
    public String format;
    @ArgParserField
    public String columnNameMappings;

    @Getter
    transient String suffixName;
    transient List<String> excelHeader;
    transient List<List<Object>> excelBody;
    transient IBiConsumer<List<Map<String, Object>>, File> exporter;
    transient Map<String, String> columnNameMappingsMap = Collections.emptyMap();

    public void init() {
        if (ObjectUtil.isNotBlank(columnNameMappings)) {
            columnNameMappingsMap = JsonUtil.fromJsonObject(columnNameMappings, String.class);
        }

        if ("json".equalsIgnoreCase(format)) {
            suffixName = "jsonl";
            exporter = this::exportRowsToJson;
        } else if ("csv".equalsIgnoreCase(format)) {
            suffixName = "csv";
            exporter = this::exportRowsToCsv;
        } else if ("excel".equalsIgnoreCase(format)) {
            suffixName = "xlsx";
            exporter = this::exportRowsToExcel;
            // excel rows limit is 2^20
            excelBody = new ArrayList<>(1024);
        } else {
            throw new AbortException("unsupported format: " + format);
        }
    }

    public void exportRows(List<Map<String, Object>> rows, File outputFile) throws Exception {
        exporter.accept(rows, outputFile);
    }

    public void finishExport(File outputFile) throws Exception {
        if (ObjectUtil.isEmpty(excelHeader)) return;

        List<String> header;
        if (ObjectUtil.isEmpty(columnNameMappings)) {
            header = excelHeader;
        } else {
            header = excelHeader.stream().map(it -> columnNameMappingsMap.getOrDefault(it, it))
                    .collect(Collectors.toList());
        }
        ExcelBuilder.build().addSheet(sheet -> {
            sheet.headerStyle(new ExcelStyle().fontBold().fontHeight(14)).header(header).body(excelBody);
        }).writeTo(outputFile);
        excelHeader = null;
        excelBody = new ArrayList<>();
    }

    private void exportRowsToJson(List<Map<String, Object>> rows, File outputFile) throws Exception {
        FileUtil.writeLines(outputFile, rows.stream().map(JsonUtil::toJson).iterator(), true);
    }

    private void exportRowsToCsv(List<Map<String, Object>> rows, File outputFile) throws Exception {
        CsvUtil.write(rows.stream().map(this::mapToRow).iterator(), outputFile, true);
    }

    private void exportRowsToExcel(List<Map<String, Object>> rows, File outputFile) throws Exception {
        if (ObjectUtil.isEmpty(excelHeader)) {
            excelHeader = new ArrayList<>(rows.get(0).keySet());
        }
        for (Map<String, Object> row : rows) {
            excelBody.add(new ArrayList<>(row.values()));
        }
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