package org.dreamcat.daily.script;

import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.daily.script.ability.DataSourceAbility;
import org.dreamcat.daily.script.ability.RandomGenerateAbility;
import org.dreamcat.daily.script.base.BaseHandler;

import java.util.List;

/**
 * @author Jerry Will
 * @version 2023-03-30
 */
@ArgParserType(allProperties = true, command = "type-table")
public class TypeTableHandler extends BaseHandler {

    @ArgParserField("t")
    private List<String> types;
    @ArgParserField("P")
    private List<String> partitionTypes;

    DataSourceAbility dataSourceAbility;
    RandomGenerateAbility randomGenerateAbility;

    @Override
    public void run() throws Exception {
        dataSourceAbility.init();
        randomGenerateAbility.init(dataSourceAbility);
    }
}
