package org.dreamcat.daily.script.base;

import static org.dreamcat.common.util.ExceptionUtil.getRootCauseMessage;

import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.Pair;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.daily.script.ability.DataSourceAbility;
import org.dreamcat.daily.script.ability.DbTablesAbility;
import org.dreamcat.daily.script.ability.JdbcAbility;
import org.dreamcat.daily.script.common.AbortException;

import java.io.File;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * @author Jerry Will
 * @version 2023-08-14
 */
@Slf4j
public abstract class BaseExportHandler extends BaseHandler {

    @ArgParserField(nested = true)
    protected DataSourceAbility dataSource;
    @ArgParserField(nested = true)
    protected JdbcAbility jdbcAbility;

    @ArgParserField(nested = true)
    protected DbTablesAbility dbTablesAbility;;

    @ArgParserField
    private String outputNameRegex;
    @ArgParserField
    private String outputNameReplacement;
    @ArgParserField({"n"})
    protected int batchSize = 1000;

    protected abstract void exportTable(Connection connection, String database, String table) throws Exception;

    @Override
    public void run() throws Exception {
        dataSource.init();
        dbTablesAbility.init(dataSource, this);

        jdbcAbility.run(this::handle);
    }

    private void handle(Connection connection) throws Exception {
        List<Pair<String, String>> dbTables = dbTablesAbility.getDbTables(connection);
        for (Pair<String, String> dbTable : dbTables) {
            String database = dbTable.getFirst();
            String table = dbTable.getSecond();

            exportTable(connection, database, table);
        }
    }

    protected String formatOutputName(String database, String table, String suffix) {
        String outputName = table + "." + suffix;
        if (database != null) {
            outputName = database + "/" + outputName;
        }
        if (outputNameRegex != null && outputNameReplacement != null) {
            String replacedOutputName = outputName.replaceAll(outputNameRegex, outputNameReplacement);
            if (verbose) {
                log.info("outputName: {} -> {}", outputName, replacedOutputName);
            }
            outputName = replacedOutputName;
        }
        return outputName;
    }

    public static abstract class Appendable extends BaseExportHandler{

        protected abstract String getSuffix();

        protected abstract void exportRows(String database, String table,
                List<Map<String, Object>> rows, File outputFile) throws Exception;

        @Override
        protected void exportTable(Connection connection, String database, String table) throws Exception {
            dataSource.getRows(connection, database, table, batchSize, rows -> {
                String outputName = formatOutputName(database, table, getSuffix());
                File outputFile = new File(outputName);

                try {
                    exportRows(database, table, rows, outputFile);
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
            });
        }
    }
}
