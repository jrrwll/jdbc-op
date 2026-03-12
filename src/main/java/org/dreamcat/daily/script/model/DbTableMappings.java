package org.dreamcat.daily.script.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.daily.script.common.AbortException;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * @author Jerry Will
 * @version 2026-03-07
 */
@Getter
@Setter
public class DbTableMappings {

    @JsonAlias("database")
    private String db;

    @JsonAlias("targetDatabase")
    private String targetDb;

    private Map<String, String> tableMappings;

    public static List<DbTableMappings> parse(String dbTableMappingsFile, String dbTableMappings) {
        List<DbTableMappings> dbTableMappingsList;
        if (dbTableMappingsFile != null) {
            dbTableMappingsList = JsonUtil.fromJsonArray(
                    new File(dbTableMappingsFile), DbTableMappings.class);
            if (ObjectUtil.isEmpty(dbTableMappingsList)) {
                throw new AbortException("empty dbTableMappingsFile: " + dbTableMappingsFile);
            }
        } else if (ObjectUtil.isNotEmpty(dbTableMappings)) {
            dbTableMappingsList = JsonUtil.fromJsonArray(
                    dbTableMappings, DbTableMappings.class);
            if (ObjectUtil.isEmpty(dbTableMappingsList)) {
                throw new AbortException("empty dbTableMappings: " + dbTableMappings);
            }
        } else {
            throw new AbortException("no dbTableMappingsFile or dbTableMappings specified");
        }

        for (DbTableMappings tableMappings : dbTableMappingsList) {
            if (ObjectUtil.isEmpty(tableMappings.getDb())) {
                throw new AbortException("$.db cannot be empty");
            }
            if (ObjectUtil.isEmpty(tableMappings.getTableMappings())) {
                throw new AbortException("$.tableMappings cannot be empty");
            }
        }
        return dbTableMappingsList;
    }
}

