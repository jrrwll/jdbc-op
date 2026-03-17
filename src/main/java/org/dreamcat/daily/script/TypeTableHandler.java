package org.dreamcat.daily.script;

import static org.dreamcat.common.util.RandomUtil.randi;
import static org.dreamcat.common.util.RandomUtil.uuid32;

import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.MutableInt;
import org.dreamcat.common.Pair;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.util.ListUtil;
import org.dreamcat.common.util.MapUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.StringUtil;
import org.dreamcat.daily.script.ability.DataSourceAbility;
import org.dreamcat.daily.script.ability.DdlSqlAbility;
import org.dreamcat.daily.script.ability.JdbcAbility;
import org.dreamcat.daily.script.ability.RandomGenerateAbility;
import org.dreamcat.daily.script.base.BaseHandler;
import org.dreamcat.daily.script.common.AbortException;

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
    @ArgParserField("c")
    private List<String> columns;

    @ArgParserField(nested = true)
    DataSourceAbility dataSourceAbility;
    @ArgParserField(nested = true)
    JdbcAbility jdbcAbility;
    @ArgParserField(nested = true)
    DdlSqlAbility ddlSqlAbility;
    @ArgParserField(nested = true)
    RandomGenerateAbility randomGenerateAbility;

    @ArgParserField
    boolean compact;
    @ArgParserField
    boolean columnQuota;
    @ArgParserField({"n"})
    int batchSize = 1;
    @ArgParserField({"tc"})
    int totalCount = randi(1, 76);
    @ArgParserField(firstChar = true)
    boolean yes;

    @Override
    public void run() throws Exception {
        dataSourceAbility.init();
        randomGenerateAbility.init(dataSourceAbility);

        handle();
    }

    private void init() {
        if (ObjectUtil.isEmpty(types)) {
            types = dataSourceAbility.getAllTypes();
        }
        if (ObjectUtil.isEmpty(types)) {
            throw new AbortException("types cannot be empty, pass --types <type1> <type2> ... to specify it");
        }
        if (partitionTypes == null) partitionTypes = Collections.emptyList();

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

    private void handle() throws Exception {
        List<String> columnNames, columnTypes;
        if (ObjectUtil.isNotEmpty(columns)) {
            columnNames = columns;
            columnTypes = ListUtil.concat(types, partitionTypes);
        } else {
            Pair<List<String>, List<String>> pair = getColumnNameAndTypes();
            columnNames = pair.getFirst();
            columnTypes = pair.getSecond();
        }

        List<String> sqlList = new ArrayList<>();
        // ddl sql
        List<String> createTableSql = ddlSqlAbility.getCreateTableSql(
                tableName, columnNames, columnTypes, compact, columnQuota);
        if (!yes) {
            log.info("{}", createTableSql);
            return;
        }

        // insert sql
        List<String> insertList = new ArrayList<>();
        int rowNum = totalCount;
        while (rowNum > batchSize) {
            rowNum -= batchSize;
            insertList.add();
        }

        sqlList.addAll(createTableSql);
        sqlList.addAll(insertList);
        jdbcAbility.executeSql(sqlList, verbose, abort);
    }

    private Pair<List<String>, List<String>> getColumnNameAndTypes() {
        List<String> columnTypes = new ArrayList<>(types);
        if (ObjectUtil.isNotEmpty(partitionTypes)) {
            columnTypes.addAll(partitionTypes);
        }

        List<String> columnNames = new ArrayList<>();

        Map<String, Integer> typeCountMap = MapUtil.toCountMap(types);
        if (typeCountMap.values().stream().allMatch(i -> i == 1)) {
            columnNames = types.stream().map(this::formatType)
                    .collect(Collectors.toList());
        } else {
            Map<String, MutableInt> seqMap = new HashMap<>();
            for (String type : types) {
                columnNames.add(formatDupName(type, typeCountMap, seqMap));
            }
        }
        if (ObjectUtil.isEmpty(partitionTypes)) {
            return Pair.of(columnNames, columnTypes);
        }

        Map<String, Integer> partitionTypeCountMap = MapUtil.toCountMap(partitionTypes);
        Map<String, MutableInt> partitionSeqMap = new HashMap<>();
        for (String partitionType : partitionTypes) {
            columnNames.add(formatDupName(partitionType, partitionTypeCountMap, partitionSeqMap));
        }

        return Pair.of(columnNames, columnTypes);
    }

    private String formatDupName(String type, Map<String, Integer> countMap, Map<String, MutableInt> seqMap) {
        String columnName = formatType(type);

        if (countMap.getOrDefault(type, 1) == 1) {
            return columnName;
        }

        int seq = seqMap.computeIfAbsent(type, k -> new MutableInt(0))
                .incrAndGet();
        columnName += "_" + seq;
        return columnName;
    }

    private String formatType(String type) {
        type = type.replaceAll("\\(\\d+\\)", "")
                .replaceAll("\\(\\d+, *\\d+\\)", "")
                .replace("(", "")
                .replace(")", "")
                .replaceAll(", *", "")
                .replace(' ', '_')
                .replace(',', '_')
                .replace("(", "_");
        return StringUtil.remove(type, ")'\"");
    }
}
