package com.kids.care.common;

/**
 * 系统角色常量（与 sys_user.role 字段取值一致）
 */
public final class Roles {

    /** 园长：全部权限 */
    public static final String ADMIN = "admin";

    /** 教师：保教业务读写，系统配置只读或不可见 */
    public static final String TEACHER = "teacher";

    /** 家长：仅能访问自己孩子的数据 */
    public static final String PARENT = "parent";

    /** 全部角色 */
    public static final String[] ALL = {ADMIN, TEACHER, PARENT};

    private Roles() {
    }

    /** 角色中文名 */
    public static String label(String role) {
        if (ADMIN.equals(role)) {
            return "园长";
        }
        if (TEACHER.equals(role)) {
            return "教师";
        }
        if (PARENT.equals(role)) {
            return "家长";
        }
        return "未知角色";
    }
}
