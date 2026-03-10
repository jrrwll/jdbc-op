package org.dreamcat.daily.script.ability;

import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.MutableInt;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.io.IWriter;
import org.dreamcat.common.io.RollingAppender;
import org.dreamcat.common.text.InterpolationUtil;
import org.dreamcat.common.util.ObjectUtil;

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

    public boolean compact;

    @ArgParserField({"R"})
    String rollingFile; // such as: output_${i+100}.sql
    @ArgParserField({"M"})
    int rollingFileMaxSqlCount = Integer.MAX_VALUE;

    transient int sqlCountCounter;
    transient int rollingFileIndex = 1;

    public void run(List<String> sqlList) throws IOException {
        int size = sqlList.size();
        for (int i = 0; i < size; i++) {
            String sql = sqlList.get(i);
            if (compact) {
                if (i > 0) System.out.print(" ");
                System.out.print(sql);
            } else {
                System.out.println(sql);
            }
        }
        if (compact) System.out.println();

        if (ObjectUtil.isEmpty(rollingFile)) return;

        int expect = rollingFileMaxSqlCount - sqlCountCounter;
        int offset = 0;
        while (expect < size) {
            List<String> subSqlList = sqlList.subList(offset, expect);
            String blockFile = InterpolationUtil.formatEl(rollingFile, "i", rollingFileIndex);
            FileUtil.write(blockFile, String.join("\n", subSqlList) + "\n", true);
            offset += expect;
            size -= expect;
            expect = rollingFileMaxSqlCount;
            sqlCountCounter = 0;
            rollingFileIndex++;
        }
        if (size > 0) {
            sqlCountCounter += size;
            List<String> subSqlList = sqlList.subList(offset, sqlList.size());
            String blockFile = InterpolationUtil.formatEl(rollingFile, "i", rollingFileIndex);
            FileUtil.write(blockFile, String.join("\n", subSqlList) + "\n", true);
        }
    }

    private void output(List<String> sqlList, boolean verbose) throws IOException {
        if (rollingFile == null) {
            if (verbose) {
                for (String sql : sqlList) {
                    log.info("{}", sql);
                }
            }
            return;
        }

        MutableInt rollingFileIndex = new MutableInt();
        RollingAppender<String> appender = new RollingAppender<>(rollingFileMaxSqlCount, () -> IWriter.of(
                new File(InterpolationUtil.formatEl(rollingFile, "i", rollingFileIndex))));
        for (String sql : sqlList) {
            if (verbose) {
                log.info("{}", sql);
            }
            appender.append(sql);
        }
    }
}
