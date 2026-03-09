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

    protected String database;
}
