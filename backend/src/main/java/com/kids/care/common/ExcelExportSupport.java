package com.kids.care.common;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.style.column.AbstractColumnWidthStyleStrategy;
import org.apache.poi.ss.usermodel.Cell;

import java.io.OutputStream;
import java.util.List;

/**
 * Excel 导出支撑：动态表头 + 列宽自适应（带上下限）。
 *
 * <p><b>为什么不用 EasyExcel 自带的 {@code LongestMatchColumnWidthStyleStrategy}：</b>
 * 它只按最长内容撑列宽、不封顶。家庭住址、过敏史这类字段最长允许 200 字，
 * 一旦某个幼儿填了长地址，整列就被撑到几百字符宽，导出的表反而没法看。
 * 这里把宽度限制在 {@value #MIN_COL_WIDTH}~{@value #MAX_COL_WIDTH} 之间，
 * 超长内容交给 Excel 自己换行（表头已冻结，横向滚动即可）。
 *
 * <p>采用动态表头（{@code head(List<List<String>>)}）而不是注解 POJO：
 * 导出列是展示用的，全部按文本输出——手机号写成数字会变科学计数法，
 * 日期写成日期类型又会受机器区域设置影响，文本最稳定。
 */
public final class ExcelExportSupport {

    /** 最小列宽（字符数），太窄会把表头挤成两行 */
    private static final int MIN_COL_WIDTH = 10;
    /** 最大列宽（字符数），再宽就得横向滚动才看得全，不如让单元格自己换行 */
    private static final int MAX_COL_WIDTH = 36;
    /** 列宽额外留白，避免文字顶到单元格边框 */
    private static final int PADDING = 2;
    /** POI 列宽单位：1 个字符 = 256 */
    private static final int EXCEL_WIDTH_UNIT = 256;
    /** 中日韩字符从这个码点起算，按 2 个字符宽估算 */
    private static final int CJK_START = 0x2E80;

    private ExcelExportSupport() {
    }

    /**
     * 写出一个单 sheet 的 xlsx。
     *
     * @param headers 表头，顺序即列顺序
     * @param rows    数据行，列数应与表头一致
     */
    public static void write(OutputStream out, String sheetName,
                             List<String> headers, List<List<Object>> rows) {
        EasyExcel.write(out)
                .head(headers.stream().map(List::of).toList())
                .registerWriteHandler(new AutoColumnWidth(computeWidths(headers, rows)))
                .sheet(sheetName)
                .doWrite(rows);
    }

    /** 按表头与真实数据算列宽，逐列取最大值后夹在上下限之间 */
    private static int[] computeWidths(List<String> headers, List<List<Object>> rows) {
        int[] widths = new int[headers.size()];
        for (int i = 0; i < headers.size(); i++) {
            widths[i] = textWidth(headers.get(i));
        }
        for (List<Object> row : rows) {
            for (int i = 0; i < row.size() && i < widths.length; i++) {
                widths[i] = Math.max(widths[i], textWidth(row.get(i)));
            }
        }
        for (int i = 0; i < widths.length; i++) {
            widths[i] = Math.min(MAX_COL_WIDTH, Math.max(MIN_COL_WIDTH, widths[i] + PADDING));
        }
        return widths;
    }

    /** 字符串显示宽度：中日韩字符占 2 格，其余占 1 格 */
    private static int textWidth(Object value) {
        if (value == null) {
            return 0;
        }
        String s = value.toString();
        int width = 0;
        for (int i = 0; i < s.length(); i++) {
            width += s.charAt(i) >= CJK_START ? 2 : 1;
        }
        return width;
    }

    /** 固定列宽写入器：每个单元格写一次，值由数据预先算好 */
    private static final class AutoColumnWidth extends AbstractColumnWidthStyleStrategy {

        private final int[] widths;

        private AutoColumnWidth(int[] widths) {
            this.widths = widths;
        }

        @Override
        protected void setColumnWidth(WriteSheetHolder sheetHolder, List<WriteCellData<?>> cellDataList,
                                     Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {
            int columnIndex = cell.getColumnIndex();
            if (columnIndex >= 0 && columnIndex < widths.length) {
                sheetHolder.getSheet().setColumnWidth(columnIndex, widths[columnIndex] * EXCEL_WIDTH_UNIT);
            }
        }
    }
}
