package com.kids.care.common;

import com.kids.care.entity.Child;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 幼儿档案完整度计算。
 *
 * <p><b>设计原则：完整度是「算出来的」，不落库。</b>
 * 一旦存进 {@code child} 表，任何字段被修改后分数就与实际不符，还得额外维护同步逻辑；
 * 查询时实时计算永远准确，也不用改表结构。
 *
 * <p><b>权重依据「缺失后的业务风险」排序，不是平均分：</b>
 * 家长电话最重（突发情况联系不上）、过敏史次之（膳食模块直接读取该字段）、住址最轻。
 *
 * <p>本类同时对外提供 SQL 条件（{@link #incompleteSql()}），保证
 * 「列表展示的分数」与「待补全筛选」用的是同一份权重定义。
 */
public final class ChildCompleteness {

    /** 满分 */
    public static final int FULL_SCORE = 100;

    /**
     * 字段定义：中文名 / 数据库列名 / 权重 / 是否文本字段。
     * 文本字段的空串视为「未填写」；非文本字段（日期、ID）判空即可。
     */
    private record Item(String label, String column, int weight, boolean text) {}

    private static final List<Item> ITEMS = List.of(
            new Item("幼儿姓名", "name", 10, true),
            new Item("性别", "gender", 10, true),
            new Item("出生日期", "birthday", 10, false),
            new Item("所在班级", "clazz_id", 15, false),
            new Item("家长姓名", "guardian_name", 15, true),
            new Item("家长电话", "guardian_phone", 15, true),
            new Item("家庭住址", "address", 5, true),
            new Item("过敏史", "allergy", 10, true),
            new Item("入园日期", "enroll_date", 10, false)
    );

    private ChildCompleteness() {
    }

    /** 逐字段取值，key 与 {@link Item#column()} 对应 */
    private static Map<String, Object> values(Child c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", c.getName());
        m.put("gender", c.getGender());
        m.put("birthday", c.getBirthday());
        m.put("clazz_id", c.getClazzId());
        m.put("guardian_name", c.getGuardianName());
        m.put("guardian_phone", c.getGuardianPhone());
        m.put("address", c.getAddress());
        m.put("allergy", c.getAllergy());
        m.put("enroll_date", c.getEnrollDate());
        return m;
    }

    private static boolean filled(Object value, boolean text) {
        if (value == null) {
            return false;
        }
        return !text || !value.toString().trim().isEmpty();
    }

    /** 计算完整度得分（0~100） */
    public static int score(Child c) {
        Map<String, Object> v = values(c);
        int s = 0;
        for (Item it : ITEMS) {
            if (filled(v.get(it.column()), it.text())) {
                s += it.weight();
            }
        }
        return s;
    }

    /** 缺失字段的中文说明，如「家长电话、过敏史」；返回空串表示档案已完整 */
    public static String missingLabels(Child c) {
        Map<String, Object> v = values(c);
        StringJoiner sj = new StringJoiner("、");
        for (Item it : ITEMS) {
            if (!filled(v.get(it.column()), it.text())) {
                sj.add(it.label());
            }
        }
        return sj.toString();
    }

    /**
     * 未补全的 SQL 条件：完整度 &lt; 100。
     * 由同一份 {@link #ITEMS} 生成，避免 Java 与 SQL 两套权重各写一遍导致不一致。
     */
    public static String incompleteSql() {
        StringBuilder sb = new StringBuilder("(");
        for (Item it : ITEMS) {
            sb.append("(CASE WHEN ").append(it.column()).append(" IS NOT NULL");
            if (it.text()) {
                sb.append(" AND TRIM(").append(it.column()).append(") <> ''");
            }
            sb.append(" THEN ").append(it.weight()).append(" ELSE 0 END) + ");
        }
        sb.append("0) < ").append(FULL_SCORE);
        return sb.toString();
    }
}
