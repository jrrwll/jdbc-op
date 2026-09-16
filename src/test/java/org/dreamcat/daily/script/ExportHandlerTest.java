package org.dreamcat.daily.script;

import org.junit.jupiter.api.Test;

/**
 * @author Jerry Will
 * @version 2026-09-17
 */
public class ExportHandlerTest extends BaseTest {

    @Test
    void testHelp() {
        run("export", "-h");
    }
}
