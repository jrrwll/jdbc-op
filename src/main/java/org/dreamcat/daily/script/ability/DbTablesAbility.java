package org.dreamcat.daily.script.ability;

import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.Pair;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.daily.script.base.BaseHandler;
import org.dreamcat.daily.script.model.DbTables;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jerry Will
 * @version 2026-03-07
 */
@Slf4j
@ArgParserType(allProperties = true)
public class DbTablesAbility {

    @ArgParserField("dtf")
    public String dbTablesFile;
    @ArgParserField("dt")
    public String dbTables; // json content

    transient List<DbTables> dbTablesList;

    transient DataSourceAbility dataSource;
    transient BaseHandler handler;

    public void init(DataSourceAbility dataSource, BaseHandler handler) {
        this.dataSource = dataSource;
        this.handler = handler;
    }

    public List<Pair<String, String>> getDbTables(Connection connection) throws Exception {
        List<Pair<String, String>> result = new ArrayList<>();
        List<String> databases = dataSource.getDatabases(connection);
        if (ObjectUtil.isEmpty(databases)) {
            log.warn("no databases found");
            return result;
        }

        return result;
    }

}
