package com.kids.care.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kids.care.entity.Fee;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收费记录 Mapper
 */
@Mapper
public interface FeeMapper extends BaseMapper<Fee> {
}
