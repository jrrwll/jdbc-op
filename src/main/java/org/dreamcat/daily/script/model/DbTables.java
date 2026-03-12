package org.dreamcat.daily.script.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.daily.script.common.AbortException;

import java.io.File;
import java.util.List;

/**
 * @author Jerry Will
 * @version 2026-03-07
 */
@Getter
@Setter
public class DbTables {

    @JsonAlias("database")
    private String db;

    private List<String> tables;

    public static List<DbTables> parse(String dbTablesFile, String dbTables) {
        List<DbTables> dbTablesList;
        if (dbTablesFile != null) {
            dbTablesList = JsonUtil.fromJsonArray(
                    new File(dbTablesFile), DbTables.class);
            if (ObjectUtil.isEmpty(dbTablesList)) {
                throw new AbortException("empty dbTablesFile: " + dbTablesFile);
            }
        } else if (ObjectUtil.isNotEmpty(dbTables)) {
            dbTablesList = JsonUtil.fromJsonArray(
                    dbTables, DbTables.class);
            if (ObjectUtil.isEmpty(dbTablesList)) {
                throw new AbortException("empty dbTables: " + dbTables);
            }
        } else {
            throw new AbortException("no dbTablesFile or dbTables specified");
        }
        // check
        for (DbTables tables : dbTablesList) {
            if (ObjectUtil.isEmpty(tables.getDb())) {
                throw new AbortException("$.db cannot be empty");
            }
            if (ObjectUtil.isEmpty(tables.getTables())) {
                throw new AbortException("$.tables cannot be empty");
            }
        }
        return dbTablesList;
    }
}
