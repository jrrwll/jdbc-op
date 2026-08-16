package org.dreamcat.daily.script.base;

import static org.dreamcat.common.util.RandomUtil.uuid32;

import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.Pair;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.daily.script.ability.DbTablesAbility;
import org.dreamcat.daily.script.ability.JdbcAbility;

import java.io.File;
import java.sql.Connection;
import java.util.List;

/**
 * @author Jerry Will
 * @version 2023-08-14
 */
@Slf4j
public abstract class BaseExportHandler extends BaseHandler {

    @ArgParserField(nested = true)
    protected JdbcAbility jdbcAbility;

    @ArgParserField
    protected String sql;
    @ArgParserField(nested = true)
    protected DbTablesAbility dbTablesAbility;;

    @ArgParserField({"O"})
    protected String outputFile;
    @ArgParserField
    protected String outputNameRegex; // such as: (.*)/(.*)
    @ArgParserField
    protected String outputNameReplacement;
    @ArgParserField({"n"})
    protected int batchSize = 1000;

    protected abstract void exportTable(Connection connection,
            String database, String table) throws Exception;

    protected abstract void exportSql(Connection connection,
            String sql) throws Exception;

    @Override
    public void run() throws Exception {
        jdbcAbility.run(this::handle, verbose);
    }

    private void handle(Connection connection) throws Exception {
        if (ObjectUtil.isNotEmpty(sql)) {
            exportSql(connection, sql);
            return;
        }

        List<Pair<String, String>> dbTables = dbTablesAbility.getDbTables(connection);
        for (Pair<String, String> dbTable : dbTables) {
            String database = dbTable.getFirst();
            String table = dbTable.getSecond();

            exportTable(connection, database, table);
        }
    }

    protected File getSqlOutputFile(String suffix) {
        if (outputFile != null) {
            if (outputFile.contains(".")) {
                return new File(outputFile);
            } else {
                return new File(outputFile + "." + suffix);
            }
        } else {
            return new File(uuid32() + "." + suffix);
        }
    }

    protected File getTableOutputFile(String database, String table, String suffix) {
        if (outputFile != null) {
            if (outputFile.contains(".")) {
                return new File(outputFile);
            } else {
                return new File(outputFile + "." + suffix);
            }
        }

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
        return new File(outputName);
    }
}
