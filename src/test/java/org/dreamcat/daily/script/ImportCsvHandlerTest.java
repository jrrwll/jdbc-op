package org.dreamcat.daily.script;

import org.dreamcat.common.argparse.CommandArgParser;
import org.dreamcat.common.util.ClassLoaderUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Jerry Will
 * @version 2023-06-27
 */
class ImportCsvHandlerTest extends BaseTest {

    @Test
    void testHelp() {
        run("import-csv", "-h");
    }

    @Test
    void test() throws Exception {
        run(
                "import-csv", "t_table_test", "-n", "3",
                "-f", new File(testResourceDir, "test.csv").getCanonicalPath()
        );
    }

    @Test
    void testJdbc1() throws Exception {
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList(
                "import-csv", "t_table_test", "-n", "3", "--create",
                "-f", new File(testResourceDir, "test.csv").getCanonicalPath()
        ));
        args.addAll(Arrays.asList(
                "-S", "sqlite", "-j", "jdbc:sqlite:build/temp.sqlite",
                "--dc", "org.sqlite.JDBC", "--dp"
        ));
        args.addAll(sqliteDriverPath());
        run(args);

        System.out.println("---- ---- ----    ---- ---- ----");
        args.add("--yes");
        run(args);
    }

    @Test
    void testJdbc2() throws Exception {
        Main.main("import-csv", "t_table_test", "-n", "3", "-S", "mysql",
                "-f", new File(testResourceDir, "test.csv").getCanonicalPath()
        );
    }
}
