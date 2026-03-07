package org.dreamcat.daily.script;

import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.daily.script.base.BaseHandler;

/**
 * @author Jerry Will
 * @version 2023-03-22
 */
@ArgParserType(
        allProperties = true,
        firstChar = true,
        subcommands = {
                SyncHandler.class,
                // export
                ExportCsvHandler.class,
                ExportExcelHandler.class,
                // import
                ImportCsvHandler.class,
                ImportExcelHandler.class,
                //
        })
public class Main extends BaseHandler {

    public static void main(String... args) {
        run(Main.class, args);
    }

    @Override
    public void run() throws Exception {
        System.err.println("require a subcommand");
        System.err.println("jdbc-op: try 'jdbc-op --help' for more information");
    }
}
