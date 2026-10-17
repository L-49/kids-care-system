package com.kids.care.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kids.care.entity.FoodStock;
import org.apache.ibatis.annotations.Mapper;

/**
 * 食材库存 Mapper
 */
@Mapper
public interface FoodStockMapper extends BaseMapper<FoodStock> {
}
