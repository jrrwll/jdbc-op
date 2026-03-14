package org.dreamcat.daily.script;

import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;

import java.util.List;

/**
 * @author Jerry Will
 * @version 2023-03-30
 */
@ArgParserType(allProperties = true, command = "type-table")
public class TypeTableHandler {

    @ArgParserField("t")
    private List<String> types;
    @ArgParserField("P")
    private List<String> partitionTypes;



}
