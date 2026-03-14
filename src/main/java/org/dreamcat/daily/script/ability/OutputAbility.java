package org.dreamcat.daily.script.ability;

import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.MutableInt;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.io.IWriter;
import org.dreamcat.common.io.RollingAppender;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Jerry Will
 * @version 2023-08-14
 */
@Slf4j
@ArgParserType(allProperties = true)
public class OutputAbility {

    @ArgParserField({"R"})
    String rollingFile; // such as: output_%03d.sql
    @ArgParserField({"rfo"})
    int rollingFileOffset;
    @ArgParserField({"M"})
    int rollingFileMaxSqlCount = 1000;

    public void run(List<String> sqlList, boolean verbose) throws IOException {
        if (rollingFile == null) {
            for (String sql : sqlList) {
                log.info("{}", sql);
            }
            return;
        }

        MutableInt rollingFileIndex = new MutableInt(rollingFileOffset);
        try (RollingAppender<String> appender = new RollingAppender<>(rollingFileMaxSqlCount, () -> IWriter.of(
                new File(String.format(rollingFile, rollingFileIndex.incrAndGet()))))) {
            for (String sql : sqlList) {
                if (verbose) {
                    log.info("{}", sql);
                }
                appender.append(sql + "\n");
            }
        }
    }
}
