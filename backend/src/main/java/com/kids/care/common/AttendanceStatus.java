package com.kids.care.common;

import java.util.List;

/**
 * 考勤状态常量：业务代码与统计 SQL 共用同一处定义，避免中文状态各处硬编码。
 */
public final class AttendanceStatus {

    public static final String PRESENT = "出勤";
    public static final String LATE = "迟到";
    public static final String LEAVE = "请假";
    public static final String ABSENT = "缺勤";

    /** 全部合法状态 */
    public static final List<String> ALL = List.of(PRESENT, LATE, LEAVE, ABSENT);

    private AttendanceStatus() {
    }

    public static boolean isValid(String status) {
        return status != null && ALL.contains(status);
    }
}
