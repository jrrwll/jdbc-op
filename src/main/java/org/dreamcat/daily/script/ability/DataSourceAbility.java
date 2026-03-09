package org.dreamcat.daily.script.ability;

import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.Pair;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.json.YamlUtil;
import org.dreamcat.common.sql.JdbcColumnDef;
import org.dreamcat.common.sql.JdbcUtil;
import org.dreamcat.common.sql.SqlLiteralConvertor;
import org.dreamcat.common.sql.SqlValueGenerator;
import org.dreamcat.common.text.InterpolationUtil;
import org.dreamcat.common.text.TextValueType;
import org.dreamcat.common.util.ClassLoaderUtil;
import org.dreamcat.common.util.FunctionUtil;
import org.dreamcat.common.util.MapUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.StringUtil;
import org.dreamcat.daily.script.model.DataSourceInfos;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2023-08-14
 */
@Slf4j
@ArgParserType(allProperties = true)
public class DataSourceAbility {

    @ArgParserField("S")
    public String dataSourceType;

    public boolean enableNeg; // generate neg number for number types
    public double nullRatio = 0;
    public String rowNullRatio; // like this 'ratio,rows', example: 0.5,10
    public Set<String> converters; // binary:cast($value as $type)

    transient String databaseSchemaSql;
    transient String tableSchemaSql;
    transient String columnSchemaSql;
    transient String  columnCommentSql;
    transient boolean doubleQuota; // "c1" or `c2`

    transient EnumMap<TextValueType, String> textValueTypeMapping;

    transient SqlLiteralConvertor literalConvertor;
    transient SqlValueGenerator valueGenerator;

    transient RowNullRatioBasedGen rowNullRatioBasedGen;

    private static final String select_without_database_sql = "select * from $table";
    private static final String select_sql = "select * from $database.$table";;

    // ==== ==== ==== ====    ==== ==== ==== ====    ==== ==== ==== ====

    public List<String> getDatabases(Connection connection) throws SQLException {
        if (databaseSchemaSql == null) {
            return JdbcUtil.getDatabases(connection);
        }

        log.info("getDatabases: {}", databaseSchemaSql);
        return JdbcUtil.getRows(connection, databaseSchemaSql).stream()
                .map(map -> new ArrayList<>(map.values()).get(0).toString())
                .collect(Collectors.toList());
    }

    public List<String> getTables(Connection connection, String database)
            throws SQLException {
        if (tableSchemaSql == null) {
            log.info("getTables: database={}", database);
            return JdbcUtil.getTables(connection, database);
        }

        if (database == null) {
            throw new IllegalArgumentException(tableSchemaSql + " is unsupported for database-less db");
        }
        String sql = InterpolationUtil.format(tableSchemaSql,
                "database", database, "db", database);
        log.info("getTables: {}", sql);
        return JdbcUtil.getRows(connection, sql).stream()
                .map(map -> new ArrayList<>(map.values()).get(0).toString())
                .collect(Collectors.toList());
    }

    public Map<String, JdbcColumnDef> getColumnMap(Connection connection, String database, String table)
            throws SQLException {
        List<JdbcColumnDef> columns = getColumns(connection, database, table);
        return MapUtil.toMap(columns, JdbcColumnDef::getName);
    }

    public List<JdbcColumnDef> getColumns(Connection connection, String database, String table)
            throws SQLException {
        if (columnSchemaSql == null) {
            log.info("getColumns: database={}, table={}", database, table);
            return JdbcUtil.getColumns(connection, database, table);
        }

        String sql = InterpolationUtil.format(columnSchemaSql,
                "database", database, "db", database, "table", table, "tb", table);
        log.info("getColumns: {}", sql);
        return JdbcUtil.getRows(connection, sql).stream()
                .map(map -> {
                    Object name = FunctionUtil.getByIgnoreCaseKey(map::get,
                            "Field", "Column", "Name");
                    Object type = FunctionUtil.getByIgnoreCaseKey(map::get, "Type");
                    if (name == null || type == null) {
                        throw new RuntimeException("unexpect desc result: " + map);
                    }
                    return JdbcColumnDef.builder().name(name.toString())
                            .type(type.toString()).build();
                })
                .collect(Collectors.toList());
    }

    public void getRows(Connection connection, String database, String table, int batchSize,
            Consumer<List<Map<String, Object>>> handler) throws SQLException {
        String sql = InterpolationUtil.format(
                database == null ? select_without_database_sql : select_sql,
                "database", database, "db", database,
                "table", table, "tb", table);
        log.info("getRows: {}", sql);
        try (Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery(sql)) {
                JdbcUtil.getRows(rs, batchSize, rows -> {
                    log.info("handling {} rows on {}.{}",
                            rows.size(), database, table);
                    if (!rows.isEmpty()) {
                        handler.accept(rows);
                    }
                });
            }
        }
    }

    // ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----

    public String getInsertIntoSql(
            List<List<Object>> rows,
            List<String> columnNames, List<String> columnTypes,
            String database, String table, boolean columnQuota) {
        String columnNameSql = StringUtil.join(",", columnNames, columnName -> {
            if (!columnQuota) return columnName;
            return StringUtil.escape(columnName, doubleQuota ? "\"" : "`");
        });
        String insertIntoSql;
        if (database != null) {
            insertIntoSql = String.format(
                    "insert into %s.%s(%s) values ", database, table, columnNameSql);
        } else {
            insertIntoSql = String.format(
                    "insert into %s(%s) values ", table, columnNameSql);
        }

        return insertIntoSql + generateValues(rows, columnTypes);
    }

    public String getColumnCommentSql(String comment) {
        if (columnCommentSql == null) return null;
        return InterpolationUtil.format(columnCommentSql, "comment", comment);
    }

    public String formatColumnName(String columnName, boolean columnQuota) {
        if (!columnQuota) return columnName;
        return StringUtil.escape(columnName, doubleQuota ? "\"" : "`");
    }

    public List<String> detectColumnTypes(List<Object> values) {
        return values.stream().map(this::detectColumnType).collect(Collectors.toList());
    }

    public String detectColumnType(Object value) {
        TextValueType textValueType = TextValueType.detectObject(value);
        return textValueTypeMapping.get(textValueType);
    }

    // ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----

    public String generateValues(List<List<Object>> rows, List<String> typeNames) {
        return literalConvertor.generateValues(rows, typeNames);
    }

    public String generateLiteral(String typeName) {
        Object value = valueGenerator.generate(typeName);
        return literalConvertor.convertAsLiteral(value, typeName);
    }

    public String nullLiteral() {
        return literalConvertor.getNullLiteral();
    }

    // ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----

    public void reset(int columns) {
        if (rowNullRatioBasedGen != null) {
            rowNullRatioBasedGen.reset(columns);
        }
    }

    public void init() throws Exception {
        initDataSourceInfos();

        // row null ratio
        if (ObjectUtil.isNotBlank(rowNullRatio)) {
            Pair<Double, Integer> pair = Pair.fromSep(rowNullRatio, ",",
                    Double::valueOf, Integer::valueOf);
            if (!pair.isFull()) {
                throw new IllegalArgumentException("invalid smartRowNullRatio: " + rowNullRatio);
            }
            double ratio = pair.first();
            int rows = pair.second();
            this.rowNullRatioBasedGen = new RowNullRatioBasedGen(ratio, rows);
        }
    }

    // ==== ==== ==== ====    ==== ==== ==== ====    ==== ==== ==== ====

    private void initDataSourceInfos() throws Exception {
        valueGenerator = new SqlValueGenerator();
        valueGenerator.setEnableNeg(enableNeg);

        literalConvertor = new SqlLiteralConvertor();
        literalConvertor.setGlobalConvertor(this::convert);

        if (dataSourceType == null) return;

        DataSourceInfos dataSourceInfos = YamlUtil.fromJson(ClassLoaderUtil.getResourceAsString(
                "datasource.yaml"), DataSourceInfos.class);
        // alias
        for (Entry<String, List<String>> entry : dataSourceInfos.getDataSourceAlias().entrySet()) {
            if (entry.getValue().contains(dataSourceType)) {
                dataSourceType = entry.getKey();
                break;
            }
        }

        // register builtin cast literal
        for (Entry<String, Map<String, List<String>>> entry : dataSourceInfos.getCastLiteral().entrySet()) {
            String template = entry.getKey();
            for (String columnType : entry.getValue().getOrDefault(dataSourceType, Collections.emptyList())) {
                registerLiteralConvertor(columnType, template);
            }
        }
        // register custom cast literal
        if (ObjectUtil.isNotEmpty(converters)) {
            for (String converter : converters) {
                String[] ss = converter.split(",", 2);
                if (ss.length != 2) {
                    throw new IllegalArgumentException("invalid converter: " + converter);
                }
                String type = ss[0], template = ss[1];
                registerLiteralConvertor(type, template);
            }
        }

        // schema sql
        for (Entry<String, List<String>> entry : dataSourceInfos.getDatabaseSchemaSql().entrySet()) {
            if (entry.getValue().contains(dataSourceType)) {
                databaseSchemaSql = entry.getKey();
            }
        }
        for (Entry<String, List<String>> entry : dataSourceInfos.getTableSchemaSql().entrySet()) {
            if (entry.getValue().contains(dataSourceType)) {
                tableSchemaSql = entry.getKey();
            }
        }
        for (Entry<String, List<String>> entry : dataSourceInfos.getColumnSchemaSql().entrySet()) {
            if (entry.getValue().contains(dataSourceType)) {
                columnSchemaSql = entry.getKey();
            }
        }

        // other
        for (Entry<String, List<String>> entry : dataSourceInfos.getColumnSchemaSql().entrySet()) {
            if (entry.getValue().contains(dataSourceType)) {
                columnCommentSql = entry.getKey();
            }
        }

        doubleQuota = dataSourceInfos.getDoubleQuota().contains(dataSourceType);
    }

    private String convert(String literal, String typeName) {
        if (nullRatio < 1 && nullRatio > 0) {
            if (Math.random() <= nullRatio) {
                return literalConvertor.getNullLiteral();
            }
        }
        if (rowNullRatioBasedGen != null && rowNullRatioBasedGen.generate()) {
            return literalConvertor.getNullLiteral();
        }
        return null;
    }

    private void registerLiteralConvertor(String type, String template) {
        literalConvertor.register((literal, typeName) -> InterpolationUtil.format(
                template, MapUtil.of("value", literal, "type", type)), type);
    }

    // ==== ==== ==== ====    ==== ==== ==== ====    ==== ==== ==== ====

    private static class RowNullRatioBasedGen {

        final int rows;
        final double ratio;
        int columns;
        int total;
        int offset;

        RowNullRatioBasedGen(double ratio, int rows) {
            this.ratio = ratio;
            this.rows = rows;
        }

        void reset(int columns) {
            this.columns = columns;
            this.total = columns * rows;
            this.offset = 0;
        }

        boolean generate() {
            if (total == 0) {
                throw new IllegalStateException("must call rest once before call generate");
            }
            if (offset >= total) {
                offset = 0;
            }
            if (offset++ < columns) {
                return Math.random() <= ratio;
            }
            return false;
        }
    }
}