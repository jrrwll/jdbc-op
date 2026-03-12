package org.dreamcat.daily.script.common;

import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.Pair;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.SetUtil;
import org.dreamcat.daily.script.ability.DataSourceAbility;

import java.sql.Connection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2026-03-05
 */
@Slf4j
public class DbTableUtil {

    public static boolean checkExistingDatabases(
            DataSourceAbility dataSource,
            Connection connection, Set<String> databases) throws Exception {
        List<String> existingDatabases = dataSource.getDatabases(connection);
        if (ObjectUtil.isEmpty(existingDatabases)) {
            log.warn("no databases found");
            return true;
        }

        Set<String> noExistingDatabases = SetUtil.difference(
                databases, new HashSet<>(existingDatabases));
        if (!noExistingDatabases.isEmpty()) {
            throw new AbortException("no existing databases: " +
                    String.join(", ", noExistingDatabases));
        }
        return false;
    }

    public static List<Pair<String, String>> checkExistingTables(
            DataSourceAbility dataSource,
            Connection connection, String schema,
            String database, List<String> tables) throws Exception {
        List<String> existingTables = dataSource.getTables(connection, schema);
        Set<String> noExistingTables = SetUtil.difference(
                new HashSet<>(tables), new HashSet<>(existingTables));
        if (noExistingTables.isEmpty()) {
            return tables.stream()
                    .map(table -> Pair.of(database, table))
                    .collect(Collectors.toList());
        } else {
            if (database != null) {
                throw new AbortException("no existing tables on " + database + ": " +
                        String.join(", ", noExistingTables));
            } else {
                throw new AbortException("no existing tables: " +
                        String.join(", ", noExistingTables));
            }
        }
    }
}
