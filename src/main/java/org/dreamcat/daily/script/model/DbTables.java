package org.dreamcat.daily.script.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

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

    private String tableRegex;
}
