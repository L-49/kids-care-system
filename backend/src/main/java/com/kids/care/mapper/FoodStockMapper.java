package com.kids.care.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kids.care.entity.FoodStock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 食材库存 Mapper
 */
@Mapper
public interface FoodStockMapper extends BaseMapper<FoodStock> {

    /**
     * 原子增减库存：delta 为正表示入库、为负表示出库。
     * 条件 quantity + delta >= 0 保证出库不会把库存减成负数，影响行数为 0 即库存不足。
     */
    @Update("update food_stock set quantity = quantity + #{delta}, update_time = now() "
            + "where id = #{id} and deleted = 0 and quantity + #{delta} >= 0")
    int addQuantity(@Param("id") Long id, @Param("delta") BigDecimal delta);
}
