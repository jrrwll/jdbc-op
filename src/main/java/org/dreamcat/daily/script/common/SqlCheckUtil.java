package org.dreamcat.daily.script.common;

import static org.dreamcat.common.util.ExceptionUtil.getRootCauseMessage;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.*;
import net.sf.jsqlparser.parser.*;

/**
 * @author Jerry Will
 * @version 2026-03-05
 */
@Slf4j
public class SqlCheckUtil {

    public static void check(String ddlSql, String datasourceType) {
        try {
            CCJSqlParserUtil.parse(ddlSql,
                    parser -> config(parser, datasourceType));
        } catch (JSQLParserException e) {
            if (e.getCause() instanceof ParseException) {
                ParseException pe = (ParseException) e.getCause();
                Token ct = pe.currentToken;

                log.error("invalid sql at {}:{} to {}:{}, error: {}",
                        ct.beginLine, ct.beginColumn, ct.endLine, ct.endColumn,
                        getRootCauseMessage(pe));
            }
        }
    }

    private static void config(CCJSqlParser parser, String datasourceType) {
        switch (datasourceType) {
            case "mysql":
            case "doris":
            case "starrocks":
                parser.withBackslashEscapeCharacter();
                break;
            case "sqlserver":
                parser.withSquareBracketQuotation();
                break;
        }
    }
}
