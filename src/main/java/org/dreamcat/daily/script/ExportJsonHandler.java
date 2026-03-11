package org.dreamcat.daily.script;

import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.daily.script.base.BaseExportHandler;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * @author Jerry Will
 * @version 2026-03-11
 */
@ArgParserType(command = "export-json")
public class ExportJsonHandler extends BaseExportHandler.Appendable {

    @Override
    protected String getSuffix() {
        return "jsonl";
    }

    @Override
    protected void exportRows(String database, String table,
            List<Map<String, Object>> rows, File outputFile) throws Exception {
        FileUtil.writeLines(outputFile, rows.stream()
                .map(JsonUtil::toJson)
                .iterator(), true);
    }
}
