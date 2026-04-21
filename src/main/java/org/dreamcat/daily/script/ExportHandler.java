package org.dreamcat.daily.script;

import static org.dreamcat.common.util.ExceptionUtil.getRootCauseMessage;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.daily.script.ability.DataSourceAbility;
import org.dreamcat.daily.script.ability.ExportAbility;
import org.dreamcat.daily.script.base.BaseExportHandler;
import org.dreamcat.daily.script.common.AbortException;

import java.io.File;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * @author Jerry Will
 * @version 2026-03-05
 */
@Slf4j
@ArgParserType(command = "export")
public class ExportHandler extends BaseExportHandler {

    @Getter
    @ArgParserField(nested = true)
    DataSourceAbility dataSourceAbility;
    @ArgParserField(nested = true)
    ExportAbility exportAbility;

    @Override
    public void exportTable(Connection connection, String database, String table) throws Exception {
        String suffixName = exportAbility.getSuffixName();
        File file  = getTableOutputFile(database, table, suffixName);

        try {
            dataSourceAbility.getRows(connection, database, table, batchSize, rows -> {
                exportRows(rows, file);
            });
            exportAbility.finishExport(file);
        } catch (Exception e) {
            String msg = "failed to export " + database + "." + table + " to " +
                    outputFile + ": " + getRootCauseMessage(e);
            if (abort) {
                throw new AbortException(msg, e);
            } else {
                if (verbose) {
                    log.error(msg);
                } else {
                    log.error(msg, e);
                }
            }
        }
    }

    @Override
    public void exportSql(Connection connection, String sql) throws Exception {
        String suffixName = exportAbility.getSuffixName();
        File file = getSqlOutputFile(suffixName);

        dataSourceAbility.getRows(connection, sql, batchSize, rows -> {
            exportRows(rows, file);
        });
        exportAbility.finishExport(file);
    }

    private void exportRows(List<Map<String, Object>> rows, File outputFile) {
        try {
            exportAbility.exportRows(rows, outputFile);
        } catch (Exception e) {
            String msg = "failed to export sql to " +
                    outputFile + ": " + getRootCauseMessage(e);
            throw new AbortException(msg, e);
        }
    }
}
