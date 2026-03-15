package org.dreamcat.daily.script;

import static org.dreamcat.common.util.DateUtil.addDay;
import static org.dreamcat.common.util.DateUtil.ofDate;
import static org.dreamcat.common.util.RandomUtil.choose36;
import static org.dreamcat.common.util.RandomUtil.rand;
import static org.dreamcat.common.util.RandomUtil.randi;
import static org.dreamcat.common.util.RandomUtil.uuid32;

import lombok.Data;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.dreamcat.common.excel.annotation.ExcelColumn;
import org.dreamcat.common.excel.annotation.ExcelColumnStyle;
import org.dreamcat.common.excel.annotation.ExcelType;
import org.dreamcat.common.excel.build.ExcelBuilder;
import org.dreamcat.common.util.FunctionUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author Jerry Will
 * @version 2023-06-30
 */
class ImportExcelHandlerTest extends BaseTest {

    String filename = FunctionUtil.invoke(() ->
            new File("build/all_type.xlsx").getCanonicalPath());

    @Test
    void testHelp() {
        run("import-excel", "-h");
    }

    @Test
    void test() throws Exception {
        Main.main(
                "import-excel", "-n", "3", "-S", "mysql",
                "-f", filename,
                "--scn", "t_table_1:c_int,c_double,c_string,c_bool,c_date,c_local_date,c_local_date_time,c_null"
        );
    }

    @Test
    void generateExcelFile() throws Exception {
        System.out.println("generate excel file to " + filename);
        ExcelBuilder.build()
                .addSheet(Pojo.class, sheet -> {
                    List<Pojo> pojoList = new ArrayList<>();
                    for (int i = 0; i < randi(1, 17); i++) {
                        pojoList.add(new Pojo());
                    }
                    sheet.body(pojoList);
                })
                .addSheet(sheet -> {
                    List<List<Object>> body = new ArrayList<>();
                    for (int i = 0; i < randi(1, 17); i++) {
                        body.add(Arrays.asList(uuid32(), addDay(new Date(), -i - 1)));
                    }
                    sheet.header(Arrays.asList("cell_a", "cell_b"))
                            .body(body);
                })
                .writeTo(new File(filename));
    }

    @ExcelType(name = "Sheet One")
    @Data
    public static class Pojo {

        @ExcelColumn(header = "Cell int", headerStyle = @ExcelColumnStyle(
                fillColorIndex = IndexedColors.RED1))
        int a = randi(128);
        @ExcelColumn(header = "Cell Double", headerStyle = @ExcelColumnStyle(
                fillColorIndex = IndexedColors.BRIGHT_GREEN1))
        Double b = rand();
        @ExcelColumn(header = "Cell String", headerStyle = @ExcelColumnStyle(
                fillColorIndex = IndexedColors.BLUE1))
        String c = choose36(randi(3, 7));
        @ExcelColumn(header = "Cell boolean", headerStyle = @ExcelColumnStyle(
                fillColorIndex = IndexedColors.YELLOW1))
        boolean d = rand() > 0.5;
        @ExcelColumn(header = "Cell Date", headerStyle = @ExcelColumnStyle(
                fillColorIndex = IndexedColors.PINK1, dataFormat = "yyyy-MM-dd hh:mm:ss"))
        Date e = new Date(System.currentTimeMillis() + randi(-3 * 24 * 3600L, 3 * 24 * 3600L));
        @ExcelColumn(header = "Cell LocalDate", headerStyle = @ExcelColumnStyle(
                fillColorIndex = IndexedColors.TURQUOISE1, dataFormat = "yyyy-MM-dd"))
        LocalDate f = ofDate(
                new Date(System.currentTimeMillis() + randi(-3 * 24 * 3600L, 3 * 24 * 3600L))).toLocalDate();
        @ExcelColumn(header = "Cell LocalDateTime", headerStyle = @ExcelColumnStyle(
                fillColorIndex = IndexedColors.GREEN, dataFormat = "yyyy-MM-dd hh:mm:ss"))
        LocalDateTime g = ofDate(new Date(System.currentTimeMillis() + randi(-3 * 24 * 3600L, 3 * 24 * 3600L)));
        @ExcelColumn(header = "null", headerStyle = @ExcelColumnStyle(
                fillColorIndex = IndexedColors.ROYAL_BLUE))
        String _null; // null
    }

}