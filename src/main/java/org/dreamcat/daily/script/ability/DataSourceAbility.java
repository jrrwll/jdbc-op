package org.dreamcat.daily.script.ability;

import static org.dreamcat.common.util.ClassLoaderUtil.getResourceAsString;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.Triple;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.json.YamlUtil;
import org.dreamcat.common.sql.JdbcColumnDef;
import org.dreamcat.common.sql.JdbcUtil;
import org.dreamcat.common.sql.SqlLiteralConvertor;
import org.dreamcat.common.sql.SqlValueGenerator;
import org.dreamcat.common.text.InterpolationUtil;
import org.dreamcat.common.text.TextValueType;
import org.dreamcat.common.util.FunctionUtil;
import org.dreamcat.common.util.MapUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.RandomUtil;
import org.dreamcat.common.util.StringUtil;
import org.dreamcat.daily.script.model.DataSourceInfos;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Jerry Will
 * @version 2023-08-14
 */
@Slf4j
public class DataSourceAbility {

    @ArgParserField("S")
    public String dataSourceType;
    @ArgParserField
    public Set<String> converters; // binary:cast($value as $type)

    transient String databaseSchemaSql;
    transient String tableSchemaSql;
    transient String columnSchemaSql;
    transient String columnCommentSql;
    transient boolean doubleQuota; // "c1" or `c2`

    @Getter
    transient List<String> allTypes;

    transient SqlLiteralConvertor literalConvertor;
    transient SqlValueGenerator valueGenerator;

    private static final String select_without_database_sql = "select * from $table";
    private static final String select_sql = "select * from $database.$table";
    ;

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
            List<Map<String, Object>> rows,
            Map<String, JdbcColumnDef> columnMap,
            String database, String table, boolean columnQuota) {
        List<String> columnNames = new ArrayList<>(rows.get(0).keySet());
        List<String> typeNames = columnNames.stream()
                .map(columnName -> columnMap.get(columnName).getType().toLowerCase())
                .collect(Collectors.toList());
        List<List<Object>> rowList = rows.stream()
                .map(map -> new ArrayList<>(map.values()))
                .collect(Collectors.toList());
        return getInsertIntoSql(rowList, columnNames, typeNames,
                database, table, columnQuota);
    }

    public String getInsertIntoSql(
            List<List<Object>> rows,
            List<String> columnNames, List<String> columnTypes,
            String database, String table, boolean columnQuota) {
        String databasePrefix = database != null ? database + "." : "";

        // dynamic partition
        Triple<List<String>, List<String>, String> triple = splitPartitions(columnNames, columnTypes, columnQuota);
        columnNames = triple.first();
        columnTypes = triple.second();
        String partitionSql = triple.third();

        String columnNameSql = getColumnNameSql(columnNames, columnQuota);
        String insertIntoSql = String.format(
                "insert into %s(%s)%s values ", databasePrefix + table, columnNameSql, partitionSql);
        String valuesSql = literalConvertor.generateValues(rows, columnTypes);
        return insertIntoSql + valuesSql + ";";
    }

    private Triple<List<String>, List<String>, String> splitPartitions(
            List<String> columnNames, List<String> columnTypes, boolean columnQuota) {
        List<String> noPartitionColumnNames = new ArrayList<>();
        List<String> noPartitionColumnTypes = new ArrayList<>();
        List<String> partitionColumnNames = new ArrayList<>();

        int columnCount = columnNames.size();
        for (int i = 0; i < columnCount; i++) {
            String columnName = columnNames.get(i);
            String columnType = columnTypes.get(i);

            if (columnName.startsWith("@")) {
                partitionColumnNames.add(columnName.substring(1));
            } else {
                noPartitionColumnNames.add(columnName);
                noPartitionColumnTypes.add(columnType);
            }
        }
        String partitionSql = "";
        if (!partitionColumnNames.isEmpty()) {
            partitionSql = " partition (" + getColumnNameSql(partitionColumnNames, columnQuota) + ")";
        }

        return Triple.of(noPartitionColumnNames, noPartitionColumnTypes, partitionSql);
    }

    private String getColumnNameSql(List<String> columnNames, boolean columnQuota) {
        return StringUtil.join(",", columnNames, columnName -> {
            if (!columnQuota) return columnName;
            return StringUtil.escape(columnName, doubleQuota ? "\"" : "`");
        });
    }

    public String getColumnCommentSql(String comment) {
        if (columnCommentSql == null) return null;
        return InterpolationUtil.format(columnCommentSql, "comment", comment);
    }

    public String formatColumnName(String columnName, boolean columnQuota) {
        if (!columnQuota) return columnName;
        return StringUtil.escape(columnName, doubleQuota ? "\"" : "`");
    }

    // ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----

    public List<List<Object>> generateValues(
            List<String> columnTypes, int rows,
            List<List<Object>> partitionValueList) {
        int columnCount = columnTypes.size();
        int partitionColumnCount = ObjectUtil.isEmpty(partitionValueList) ? 0 : partitionValueList.size();
        int noPartitionColumnCount = columnCount - partitionColumnCount;
        return IntStream.range(0, rows).mapToObj(k -> {
            List<Object> row = new ArrayList<>(columnCount);
            for (int i = 0; i < noPartitionColumnCount; i++) {
                row.add(valueGenerator.generate(columnTypes.get(i)));
            }
            for (int i = 0; i < partitionColumnCount; i++) {
                row.add(RandomUtil.chooseOne(partitionValueList.get(i)));
            }
            return row;
        }).collect(Collectors.toList());
    }

    // ==== ==== ==== ====    ==== ==== ==== ====    ==== ==== ==== ====

    public void init() throws Exception {
        valueGenerator = new SqlValueGenerator();

        literalConvertor = new SqlLiteralConvertor();

        if (dataSourceType == null) return;

        DataSourceInfos infos = YamlUtil.fromJson(getResourceAsString(
                "datasource.yaml"), DataSourceInfos.class);
        // alias
        for (Entry<String, List<String>> entry : infos.getDatasourceAlias().entrySet()) {
            if (entry.getValue().contains(dataSourceType)) {
                dataSourceType = entry.getKey();
                break;
            }
        }

        // register builtin cast literal
        for (Entry<String, Map<String, List<String>>> entry : infos.getCastLiteral().entrySet()) {
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
        for (Entry<String, List<String>> entry : infos.getDatabaseSchemaSql().entrySet()) {
            if (entry.getValue().contains(dataSourceType)) {
                databaseSchemaSql = entry.getKey();
            }
        }
        for (Entry<String, List<String>> entry : infos.getTableSchemaSql().entrySet()) {
            if (entry.getValue().contains(dataSourceType)) {
                tableSchemaSql = entry.getKey();
            }
        }
        for (Entry<String, List<String>> entry : infos.getColumnSchemaSql().entrySet()) {
            if (entry.getValue().contains(dataSourceType)) {
                columnSchemaSql = entry.getKey();
            }
        }

        // other
        for (Entry<String, List<String>> entry : infos.getColumnSchemaSql().entrySet()) {
            if (entry.getValue().contains(dataSourceType)) {
                columnCommentSql = entry.getKey();
            }
        }

        doubleQuota = infos.getDoubleQuota().contains(dataSourceType);

        // all-types
        Map<String, List<String>> allTypeMaps = YamlUtil.fromJson(
                getResourceAsString("datasource-all-types.yaml"),
                new TypeReference<Map<String, List<String>>>() {
                });
        allTypes = getByDatasourceType(allTypeMaps, Collections.emptyList());
    }

    protected <T> T getByDatasourceType(Map<String, T> map, T defaultValue) {
        return map.entrySet().stream()
                .filter(e -> Arrays.asList(e.getKey().split(",")).contains(dataSourceType))
                .map(Entry::getValue)
                .findFirst().orElse(defaultValue);
    }

    private void registerLiteralConvertor(String type, String template) {
        literalConvertor.register((literal, typeName) -> InterpolationUtil.format(
                template, MapUtil.of("value", literal, "type", type)), type);
    }
}