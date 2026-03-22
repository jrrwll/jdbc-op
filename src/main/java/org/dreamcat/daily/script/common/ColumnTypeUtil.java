package org.dreamcat.daily.script.common;

import org.dreamcat.common.util.StringUtil;

/**
 * @author Jerry Will
 * @version 2026-03-05
 */
public class ColumnTypeUtil {

    public static String formatType(String type) {
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
