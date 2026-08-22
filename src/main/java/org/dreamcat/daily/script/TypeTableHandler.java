package org.dreamcat.daily.script;

import static org.dreamcat.common.util.RandomUtil.randi;
import static org.dreamcat.common.util.RandomUtil.uuid32;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.MutableInt;
import org.dreamcat.common.Pair;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.util.ListUtil;
import org.dreamcat.common.util.MapUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.StringUtil;
import org.dreamcat.daily.script.ability.DataSourceRandomGenAbility;
import org.dreamcat.daily.script.ability.DdlSqlAbility;
import org.dreamcat.daily.script.ability.JdbcAbility;
import org.dreamcat.daily.script.ability.OutputAbility;
import org.dreamcat.daily.script.base.BaseHandler;
import org.dreamcat.daily.script.common.AbortException;
import org.dreamcat.daily.script.common.ColumnTypeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2023-03-30
 */
@Slf4j
@ArgParserType(allProperties = true, command = "type-table")
public class TypeTableHandler extends BaseHandler {

    @ArgParserField(position = 1)
    private String tableName = "t_" + StringUtil.reverse(uuid32()).substring(0, 8);

    @ArgParserField("t")
    private List<String> types;
    @ArgParserField("P")
    private List<String> partitionTypes;
    @ArgParserField("pv")
    private String partitionValues;
    @ArgParserField("c")
    private List<String> columns;

    @ArgParserField(nested = true)
    DataSourceRandomGenAbility dataSourceAbility;
    @ArgParserField(nested = true)
    JdbcAbility jdbcAbility;
    @ArgParserField(nested = true)
    DdlSqlAbility ddlSqlAbility;
    @ArgParserField(nested = true)
    OutputAbility outputAbility;

    @ArgParserField
    boolean compact;
    @ArgParserField
    boolean columnQuota;
    @ArgParserField({"n"})
    int batchSize = 1;
    @ArgParserField({"C"})
    int totalCount = randi(1, 76);
    @ArgParserField(firstChar = true)
    boolean yes;

    transient String database;
    transient List<List<Object>> partitionValueList;

    @Override
    public void init() throws Exception {
        dataSourceAbility.init();
        ddlSqlAbility.init(dataSourceAbility);

        String[] dbTable = tableName.split(",", 2);
        if (dbTable.length == 2) {
            database = dbTable[0];
            tableName = dbTable[1];
        }

        if (ObjectUtil.isEmpty(types)) {
            types = dataSourceAbility.getAllTypes();
        }
        if (ObjectUtil.isEmpty(types)) {
            throw new AbortException("types cannot be empty, pass --types <type1> <type2> ... to specify it");
        }
        if (partitionTypes == null) {
            partitionTypes = Collections.emptyList();
        } else {
            if (ObjectUtil.isNotBlank(partitionValues)) {
                try {
                    partitionValueList = JsonUtil.fromJson(partitionValues, new TypeReference<List<List<Object>>>() {
                    });
                } catch (Exception e) {
                    throw new AbortException("partitionValues is not a valid json 2d array: " + e.getMessage());
                }
            }
        }

        if (ObjectUtil.isNotEmpty(columns)) {
            if (columns.size() != types.size() + partitionTypes.size()) {
                throw new AbortException("columns size must be equal to types size + partitionTypes size");
            }

            List<String> mappedColumns = new ArrayList<>(columns.size());
            mappedColumns.addAll(columns.subList(0, types.size()));

            for (String column : columns.subList(types.size(), columns.size())) {
                mappedColumns.add("@" + column); // partition column
            }
            columns = mappedColumns;
        }
    }

    @Override
    public void run() throws Exception {
        List<String> columnNames, columnTypes;
        if (ObjectUtil.isNotEmpty(columns)) {
            columnNames = columns;
            columnTypes = ListUtil.concat(types, partitionTypes);
        } else {
            Pair<List<String>, List<String>> pair = getColumnNameAndTypes();
            columnNames = pair.getFirst();
            columnTypes = pair.getSecond();
        }
        dataSourceAbility.initRandomGen(columnNames.size());

        List<String> sqlList = new ArrayList<>();
        // ddl sql
        List<String> createTableSql = ddlSqlAbility.getCreateTableSql(
                tableName, columnNames, columnTypes, compact, columnQuota);

        // insert sql
        List<String> insertList = new ArrayList<>();
        int rowNum = totalCount;
        while (rowNum > 0) {
            int rowCount = Math.min(rowNum, batchSize);
            rowNum -= batchSize;

            List<List<Object>> rows = dataSourceAbility.generateValues(
                    columnTypes, rowCount, partitionValueList);
            String insertIntoSql = dataSourceAbility.getInsertIntoSql(
                    rows, columnNames, columnTypes,
                    database, tableName, columnQuota);
            insertList.add(insertIntoSql);
        }

        sqlList.addAll(createTableSql);
        sqlList.addAll(insertList);

        if (!yes) {
            outputAbility.run(sqlList, verbose);
        } else {
            jdbcAbility.executeSql(sqlList, verbose, abort);
        }
    }

    private Pair<List<String>, List<String>> getColumnNameAndTypes() {
        List<String> columnTypes = new ArrayList<>(types);
        if (ObjectUtil.isNotEmpty(partitionTypes)) {
            columnTypes.addAll(partitionTypes);
        }

        List<String> columnNames = new ArrayList<>();

        Map<String, Integer> typeCountMap = MapUtil.toCountMap(types);
        if (typeCountMap.values().stream().allMatch(i -> i == 1)) {
            columnNames = types.stream().map(ColumnTypeUtil::formatType)
                    .map(c -> "c_" + c)
                    .collect(Collectors.toList());
        } else {
            Map<String, MutableInt> seqMap = new HashMap<>();
            for (String type : types) {
                String columnName = formatDupName(type, typeCountMap, seqMap);
                columnNames.add("c_" + columnName);
            }
        }
        if (ObjectUtil.isEmpty(partitionTypes)) {
            return Pair.of(columnNames, columnTypes);
        }

        Map<String, Integer> partitionTypeCountMap = MapUtil.toCountMap(partitionTypes);
        Map<String, MutableInt> partitionSeqMap = new HashMap<>();
        for (String partitionType : partitionTypes) {
            String columnName = formatDupName(partitionType, partitionTypeCountMap, partitionSeqMap);
            columnNames.add("p_" + columnName);
        }

        return Pair.of(columnNames, columnTypes);
    }

    private String formatDupName(String type, Map<String, Integer> countMap, Map<String, MutableInt> seqMap) {
        String columnName = ColumnTypeUtil.formatType(type);

        if (countMap.getOrDefault(type, 1) == 1) {
            return columnName;
        }

        int seq = seqMap.computeIfAbsent(type, k -> new MutableInt(0))
                .incrAndGet();
        columnName += "_" + seq;
        return columnName;
    }
}
