package org.dreamcat.daily.script;

import org.junit.jupiter.api.Test;

import java.io.File;

/**
 * @author Jerry Will
 * @version 2026-09-17
 */
public class ImportJsonHandlerTest extends BaseTest {

    @Test
    void testHelp() {
        run("import-json", "-h");
    }

    @Test
    void testJsonl() throws Exception {
        run(
                "import-json", "t_table_test", "-n", "3",
                "-f", new File(testResourceDir, "test.jsonl").getCanonicalPath()
        );
    }

    @Test
    void testJsonArray() throws Exception {
        run(
                "import-json", "t_table_test", "-n", "3", "--json",
                "-f", new File(testResourceDir, "test.json").getCanonicalPath()
        );
    }
}
