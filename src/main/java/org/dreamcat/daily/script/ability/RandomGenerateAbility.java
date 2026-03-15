package org.dreamcat.daily.script.ability;

import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.Pair;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.util.ObjectUtil;

/**
 * @author Jerry Will
 * @version 2026-03-05
 */
@Slf4j
@ArgParserType(allProperties = true)
public class RandomGenerateAbility {

    public boolean enableNeg; // generate neg number for number types
    public double nullRatio = 0;
    public String rowNullRatio; // like this 'ratio,rows', example: 0.5,10

    DataSourceAbility dataSourceAbility;

    transient RowNullRatioBasedGen rowNullRatioBasedGen;

    public String generateLiteral(String typeName) {
        Object value = dataSourceAbility.valueGenerator.generate(typeName);
        return dataSourceAbility.literalConvertor.convertAsLiteral(value, typeName);
    }

    public String nullLiteral() {
        return dataSourceAbility.literalConvertor.getNullLiteral();
    }

    // ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----

    public void reset(int columns) {
        if (rowNullRatioBasedGen != null) {
            rowNullRatioBasedGen.reset(columns);
        }
    }

    public void init() throws Exception {
        dataSourceAbility.valueGenerator.setEnableNeg(enableNeg);

        // row null ratio
        if (ObjectUtil.isNotBlank(rowNullRatio)) {
            Pair<Double, Integer> pair = Pair.fromSep(rowNullRatio, ",",
                    Double::valueOf, Integer::valueOf);
            if (!pair.isFull()) {
                throw new IllegalArgumentException("invalid smartRowNullRatio: " + rowNullRatio);
            }
            double ratio = pair.first();
            int rows = pair.second();
            this.rowNullRatioBasedGen = new RowNullRatioBasedGen(ratio, rows);
        }

        dataSourceAbility.literalConvertor.setGlobalConvertor(this::convert);
    }

    private String convert(String literal, String typeName) {
        if (nullRatio < 1 && nullRatio > 0) {
            if (Math.random() <= nullRatio) {
                return dataSourceAbility.literalConvertor.getNullLiteral();
            }
        }
        if (rowNullRatioBasedGen != null && rowNullRatioBasedGen.generate()) {
            return dataSourceAbility.literalConvertor.getNullLiteral();
        }
        return null;
    }

    private static class RowNullRatioBasedGen {

        final int rows;
        final double ratio;
        int columns;
        int total;
        int offset;

        RowNullRatioBasedGen(double ratio, int rows) {
            this.ratio = ratio;
            this.rows = rows;
        }

        void reset(int columns) {
            this.columns = columns;
            this.total = columns * rows;
            this.offset = 0;
        }

        boolean generate() {
            if (total == 0) {
                throw new IllegalStateException("must call rest once before call generate");
            }
            if (offset >= total) {
                offset = 0;
            }
            if (offset++ < columns) {
                return Math.random() <= ratio;
            }
            return false;
        }
    }
}
