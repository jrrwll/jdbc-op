package org.dreamcat.daily.script.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

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
}

