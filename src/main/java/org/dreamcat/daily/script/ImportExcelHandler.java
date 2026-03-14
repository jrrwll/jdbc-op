package org.dreamcat.daily.script;

import static org.dreamcat.common.util.CollectionUtil.mapToList;
import static org.dreamcat.common.util.ListUtil.getOrNull;

import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.Pair;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.excel.ExcelUtil;
import org.dreamcat.common.util.ExceptionUtil;
import org.dreamcat.daily.script.base.BaseImportHandler;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2023-06-28
 */
@Slf4j
@ArgParserType(command = "import-excel")
public class ImportExcelHandler extends BaseImportHandler {

    @ArgParserField("f")
    private String file;

    @ArgParserField("scn")
    private List<String> sheetColumnNames; // mapping to table name, --sn s1:c1,c2,c3

    transient List<Pair<String, List<String>>> sheetColumnNameList;

    public void init() throws Exception {
        super.init();
        sheetColumnNameList = sheetColumnNames.stream()
                .map(s -> Pair.fromSep(s, ":", s1 -> s1, s2 -> Arrays.asList(s2.split(","))))
                .collect(Collectors.toList());
    }

    @Override
    public void run() throws Exception {
        init();

        handle();
    }

    private void handle() throws Exception {
        Map<String, List<List<Object>>> sheets = ExcelUtil.parseAsMap(new File(file));
        int sheetIndex = 0;
        for (Entry<String, List<List<Object>>> entry : sheets.entrySet()) {
            String sheetName = entry.getKey();
            List<List<Object>> rows = entry.getValue();
            List<String> header = mapToList(rows.get(0), Objects::toString);
            rows = rows.subList(1, rows.size());

            Pair<String, List<String>> pair = getOrNull(sheetColumnNameList, sheetIndex);
            if (pair != null) {
                sheetName = pair.first();
                header = pair.second();
            }

            long cost = System.currentTimeMillis();
            log.info("start to import {} {}, total {}", sheetName, header, rows.size());
            try {
                importTable(sheetName, header, rows);
            } catch (Exception e) {
                log.error("failed to import {} {}: {}", sheetName, header,
                        ExceptionUtil.getRootCauseMessage(e));
                if (abort) break;
                else continue;
            }
            cost = System.currentTimeMillis() - cost;
            log.info("success to import {} {}, cost {}ms", sheetName, header, cost);

            sheetIndex++;
        }
    }
}
