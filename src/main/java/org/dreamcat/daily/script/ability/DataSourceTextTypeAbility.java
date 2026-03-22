package org.dreamcat.daily.script.ability;

import static org.dreamcat.common.util.ClassLoaderUtil.getResourceAsString;

import com.fasterxml.jackson.core.type.TypeReference;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.json.YamlUtil;
import org.dreamcat.common.text.TextValueType;
import org.dreamcat.common.util.ObjectUtil;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2026-03-21
 */
@ArgParserType(allProperties = true)
public class DataSourceTextTypeAbility extends DataSourceAbility {

    private String textTypes; // json, type like: Map<TextValueType, String>

    transient EnumMap<TextValueType, String> textValueTypeMapping;

    public List<String> detectColumnTypes(List<Object> values) {
        return values.stream().map(this::detectColumnType).collect(Collectors.toList());
    }

    private String detectColumnType(Object value) {
        if (ObjectUtil.isEmpty(textValueTypeMapping)) return null;
        TextValueType textValueType = TextValueType.detectObject(value);
        return textValueTypeMapping.get(textValueType);
    }

    @Override
    public void init() throws Exception {
        super.init();

        // text types
        Map<String, Map<String, String>> textTypeMaps = YamlUtil.fromJson(
                getResourceAsString("datasource-text-types.yaml"),
                new TypeReference<Map<String, Map<String, String>>>() {
                });
        textValueTypeMapping = new EnumMap<>(TextValueType.class);
        Map<String, String> textTypeMap = getByDatasourceType(textTypeMaps, Collections.emptyMap());
        if (ObjectUtil.isNotEmpty(textTypes)) {
            Map<String, String> customTextTypeMap = JsonUtil.fromJsonObject(textTypes, String.class);
            textTypeMap = new HashMap<>(textTypeMap);
            textTypeMap.putAll(customTextTypeMap);
        }
        for (Entry<String, String> entry : textTypeMap.entrySet()) {
            textValueTypeMapping.put(TextValueType.valueOf(entry.getKey().toUpperCase()), entry.getValue());
        }
    }
}
