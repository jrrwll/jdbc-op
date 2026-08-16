package org.dreamcat.daily.script;

import static org.dreamcat.common.util.RandomUtil.uuid32;

import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.util.StringUtil;
import org.dreamcat.daily.script.base.BaseHandler;
import org.dreamcat.daily.script.common.ColumnTypeUtil;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2023-05-29
 */
@ArgParserType(command = "hbase-type-table")
public class
HbaseTypeTableHandler extends BaseHandler {

    @ArgParserField(position = 1)
    private String tableName = "t_" + StringUtil.reverse(uuid32()).substring(0, 8);

    @ArgParserField("t")
    private List<String> types;

    @Override
    public void run() throws Exception {

        // create 'mytable', 'c_int', 'c_string', 'c_date'
        String createTableSql = String.format("create '%s', %s\n", tableName, types.stream()
                .map(type -> "'c_" + ColumnTypeUtil.formatType(type) + "'")
                .collect(Collectors.joining(", ")));

        // put 'table_name', 'row', 'colfamily:colname', 'value'

    }


}
