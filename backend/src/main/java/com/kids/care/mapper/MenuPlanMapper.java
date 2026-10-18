package com.kids.care.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kids.care.entity.MenuPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;

/**
 * 每周食谱 Mapper
 */
@Mapper
public interface MenuPlanMapper extends BaseMapper<MenuPlan> {

    /** 查找同日同餐次已被软删除的记录（唯一键 uk_menu 不含 deleted，重录时需要复活） */
    @Select("select id from menu_plan where menu_date = #{menuDate} and meal_type = #{mealType} "
            + "and deleted = 1 limit 1")
    Long findDeletedId(@Param("menuDate") LocalDate menuDate, @Param("mealType") String mealType);

    /** 复活软删除记录并覆盖最新内容 */
    @Update("update menu_plan set deleted = 0, dishes = #{dishes}, note = #{note}, "
            + "publisher = #{publisher}, create_time = now() where id = #{id}")
    int revive(@Param("id") Long id, @Param("dishes") String dishes,
               @Param("note") String note, @Param("publisher") String publisher);
}
