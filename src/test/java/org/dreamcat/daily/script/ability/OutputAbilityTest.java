package org.dreamcat.daily.script.ability;

import org.dreamcat.common.argparse.TypeArgParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Jerry Will
 * @version 2026-03-05
 */
class OutputAbilityTest {

    List<String> args = new ArrayList<>(Arrays.asList(
            "--rolling-file", "build/tmp/output_%03d.sql"));
    List<String> sqlList = Arrays.asList(
            "select 1;", "select 2;", "select 3;", "select 4;", "select 5;"
    );

    @Test
    void test1() throws Exception {
        List<String> args1 = new ArrayList<>(args);
        args1.add("--rolling-file-offset");
        args1.add("7");
        OutputAbility outputAbility = new OutputAbility();
        TypeArgParser.injectTo(outputAbility, args1.toArray(new String[0]));
        outputAbility.run(sqlList, true);
    }

    @Test
    void test2() throws Exception {
        List<String> args1 = new ArrayList<>(args);
        args1.add("--rolling-file-max-sql-count");
        args1.add("2");
        OutputAbility outputAbility = new OutputAbility();
        TypeArgParser.injectTo(outputAbility, args1.toArray(new String[0]));
        outputAbility.run(sqlList, true);
    }
}
