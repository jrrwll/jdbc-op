package org.dreamcat.daily.script.ability;

import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.sql.JdbcColumnDef;
import org.dreamcat.common.sql.JdbcUtil;
import org.dreamcat.common.text.InterpolationUtil;
import org.dreamcat.common.util.MapUtil;
import org.dreamcat.common.util.SetUtil;
import org.dreamcat.common.util.StringUtil;
import org.dreamcat.daily.script.common.AbortException;
import org.dreamcat.daily.script.common.SqlCheckUtil;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Jerry Will
 * @version 2026-03-07
 */
@Slf4j
public class DdlSqlAbility {

    String preColumnDefSql;
    String postColumnDefSql;
    String postTableDefSql;
    String columnCommentTemplate; //such as: Column Type: $type

    boolean noCheckSql;

    transient DataSourceAbility dataSourceAbility;

    public void init(DataSourceAbility dataSourceAbility) {
        this.dataSourceAbility = dataSourceAbility;
    }

    public List<String> checkAndGetCreateSql(
            Connection connection, String database, String table,
            List<String> columnNames, List<String> columnTypes,
            boolean compact, boolean columnQuota) throws Exception {
        String schema = database;
        if (database== null) {
            schema = connection.getSchema();
        }
        List<String> tableLike = JdbcUtil.getTableLike(connection, schema, table);
        if (tableLike.isEmpty()) {
            log.info("table {} does not exist, generating DDL sql to create it", table);

            if (columnTypes.stream().anyMatch(Objects::isNull)) {
                throw new AbortException("unsupported datasource " + dataSourceAbility.dataSourceType +
                        ", need custom text type mapping: --text-types <textTypes>");
            }
            return getCreateTableSql(
                    table, columnNames, columnTypes,
                    compact, columnQuota);
        }

        log.info("table {} already exists, verify the columns instead of creating it", table);

        Map<String, JdbcColumnDef> columnMap = dataSourceAbility.getColumnMap(connection, schema, table);

        Set<String> noExistingColumns = SetUtil.difference(new HashSet<>(columnNames), columnMap.keySet());
        if (!noExistingColumns.isEmpty()) {
            throw new AbortException("columns " + String.join(", ", noExistingColumns) +
                    " does not exist in table " + table);
        }
        return Collections.emptyList();
    }

    public List<String> getCreateTableSql(
            String tableName, List<String> columnNames, List<String> columnTypes,
            boolean compact, boolean columnQuota) {
        List<String> ddlList = new ArrayList<>();

        String sep = compact ? " " : "\n";
        StringBuilder createTableSql = new StringBuilder();
        createTableSql.append("create table ").append(tableName).append(" (").append(sep);
        if (StringUtil.isNotBlank(preColumnDefSql)) {
            createTableSql.append("    ").append(preColumnDefSql).append(",").append(sep);
        }

        // column
        int columnCount = columnNames.size();
        List<String> columnDefSqlList = new ArrayList<>(columnCount);
        List<String> columnCommentSqlList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            String columnName = columnNames.get(i);
            if (columnName.startsWith("@")) continue; // partitioned columns
            String columnType = columnTypes.get(i);

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
        } else {
            // only works if no --post-table-def-sql
            List<String> partitionColumnDefSqlList = new ArrayList<>();
            for (int i = 0; i < columnCount; i++) {
                String columnName = columnNames.get(i);
                if (!columnName.startsWith("@")) continue;
                String columnType = columnTypes.get(i);

                partitionColumnDefSqlList.add(columnName.substring(1) + " " + columnType);
            }
            if (!partitionColumnDefSqlList.isEmpty()) {
                createTableSql.append(sep)
                        .append("partitioned by (")
                        .append(String.join(", ", partitionColumnDefSqlList))
                        .append(")");
            }
        }
        createTableSql.append(";");
        String createTableSqlStr = createTableSql.toString();
        if (!noCheckSql) {
            SqlCheckUtil.check(createTableSqlStr, dataSourceAbility.dataSourceType);
        }
        ddlList.add(createTableSqlStr);

        for (String s : columnCommentSqlList) {
            if (!s.endsWith(";")) s += ";";
            ddlList.add(s);
        }
        return ddlList;
    }

}
