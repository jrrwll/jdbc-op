package org.dreamcat.daily.script.base;

import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.daily.script.ability.*;

/**
 * @author Jerry Will
 * @version 2023-06-27
 */
public abstract class BaseImportHandler extends BaseHandler {

    @ArgParserField(nested = true)
    protected JdbcAbility jdbcAbility;
    @ArgParserField(nested = true)
    protected DataSourceAbility dataSourceAbility;
    @ArgParserField(nested = true)
    protected OutputAbility outputAbility;

    protected String database;
    @ArgParserField({"n"})
    protected int batchSize = 1000;

    protected boolean columnQuota;
    protected boolean yes;

    public void init() throws Exception {
        dataSourceAbility.init();
    }
}
