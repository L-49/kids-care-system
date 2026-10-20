package com.kids.care.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kids.care.entity.Child;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 幼儿档案 Mapper
 */
@Mapper
public interface ChildMapper extends BaseMapper<Child> {

    /**
     * 各班在园人数（一次 group by 聚合，避免班级列表逐班 count）。
     * 合并组员1「班级管理」模块时新增，供 ClazzServiceImpl 补齐 childCount / fullStatus。
     * 口径：未删除 + 在园 + 已分班。在园按入园日期实时判定（晚于今天为预约、不计入），
     * 与幼儿列表、工作台保持同一口径，避免班级容量看着还有空位却调不进去。
     */
    @Select("select clazz_id as clazzId, count(*) as num from child "
            + "where deleted = 0 and clazz_id is not null "
            + "and (case when enroll_date is not null then (case when enroll_date > curdate() then 0 else 1 end) else status end) = 1 "
            + "group by clazz_id")
    List<Map<String, Object>> countByClazz();
}
