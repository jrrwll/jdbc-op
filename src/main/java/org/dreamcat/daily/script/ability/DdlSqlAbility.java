package org.dreamcat.daily.script.ability;

import org.dreamcat.common.text.InterpolationUtil;
import org.dreamcat.common.util.MapUtil;
import org.dreamcat.common.util.StringUtil;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author Jerry Will
 * @version 2026-03-07
 */
public class DdlSqlAbility {

    String preColumnDefSql;
    String postColumnDefSql;
    String postTableDefSql;

    String columnCommentTemplate; //such as: Column Type: $type

    transient DataSourceAbility dataSourceAbility;

    public List<String> getCreateTableSql(String tableName, Map<String, String> columns, boolean compact,
            boolean columnQuota) {
        List<String> ddlList = new ArrayList<>();

        String sep = compact ? " " : "\n";
        StringBuilder createTableSql = new StringBuilder();
        createTableSql.append("create table ").append(tableName).append(" (").append(sep);
        if (StringUtil.isNotBlank(preColumnDefSql)) {
            createTableSql.append("    ").append(preColumnDefSql).append(",").append(sep);
        }

        // column
        int columnCount = columns.size();
        List<String> columnDefSqlList = new ArrayList<>(columnCount);
        List<String> columnCommentSqlList = new ArrayList<>(columnCount);

        List<String> columnNames = new ArrayList<>(columnCount);
        for (Entry<String, String> entry : columns.entrySet()) {
            String columnName = entry.getKey();
            String columnType = entry.getValue();

            String columnDefSql = dataSourceAbility.formatColumnName(columnName, columnQuota) + " " + columnType;
            if (!compact) columnDefSql = "    " + columnDefSql;

            if (StringUtil.isNotBlank(columnCommentTemplate)) {
                Map<String, Object> ctx = MapUtil.of(
                        "table", tableName, "column", columnName, "type", columnType);
                String comment = InterpolationUtil.format(columnCommentTemplate, ctx);
                comment = StringUtil.escape(comment, "'");

                String columnCommentSql = dataSourceAbility.getColumnCommentSql(comment);
                if (columnCommentSql != null) {
                    columnCommentSqlList.add(columnCommentSql);
                } else {
                    columnDefSql = String.format("%s comment '%s'", columnDefSql, comment);
                }
            }

            columnDefSqlList.add(columnDefSql);
        }
        createTableSql.append(String.join("," + sep, columnDefSqlList))
                .append(sep).append(")");
        if (StringUtil.isNotBlank(postColumnDefSql)) {
            createTableSql.append("    ").append(postColumnDefSql).append(sep);
        }

        if (StringUtil.isNotBlank(postTableDefSql)) {
            createTableSql.append(" ").append(postTableDefSql);
        }
        createTableSql.append(";");
        ddlList.add(createTableSql.toString());

        for (String s : columnCommentSqlList) {
            if (!s.endsWith(";")) s += ";";
            ddlList.add(s);
        }
        return ddlList;
    }

}
