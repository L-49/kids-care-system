package com.kids.care.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel 导入结果。
 *
 * <p>采用「逐行校验、成功入库、失败报行号」的策略：
 * 50 条里错了 3 条，不应该让用户整批重来。若需要强一致（有错则全不写），
 * 在 service 里把 saveBatch 挪到校验全部通过之后即可。
 */
@Data
public class ImportResult {

    /** 实际参与校验的行数（不含表头与空行） */
    private int total;

    /** 成功入库条数 */
    private int successCount;

    /** 失败条数 */
    private int failCount;

    /** 失败明细，按 Excel 行号排序 */
    private List<RowError> errors = new ArrayList<>();

    public void addError(int rowNum, String name, String message) {
        this.errors.add(new RowError(rowNum, name, message));
        this.failCount = this.errors.size();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowError {
        /** Excel 中的行号（表头是第 1 行，从 2 开始） */
        private int rowNum;
        /** 幼儿姓名，便于用户定位 */
        private String name;
        /** 具体原因 */
        private String message;
    }
}
