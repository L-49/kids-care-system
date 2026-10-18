package com.kids.care.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kids.care.entity.StockRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 食材出入库流水 Mapper
 */
@Mapper
public interface StockRecordMapper extends BaseMapper<StockRecord> {

    /** 本月出入库汇总：type -> 笔数 / 数量 */
    @Select("select type, count(*) as times, ifnull(sum(quantity), 0) as qty "
            + "from stock_record where date_format(create_time, '%Y-%m') = date_format(curdate(), '%Y-%m') "
            + "group by type")
    java.util.List<Map<String, Object>> monthSummary();

    /** 指定食材最近一次流水（用于展示"上次操作"） */
    @Select("select * from stock_record where food_id = #{foodId} order by id desc limit 1")
    StockRecord lastOfFood(@Param("foodId") Long foodId);

    /** 某类操作的累计数量 */
    @Select("select ifnull(sum(quantity), 0) from stock_record where type = #{type}")
    BigDecimal sumByType(@Param("type") String type);
}
