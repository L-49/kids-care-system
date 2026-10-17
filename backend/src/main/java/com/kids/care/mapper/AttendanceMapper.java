package com.kids.care.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kids.care.entity.Attendance;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考勤记录 Mapper
 */
@Mapper
public interface AttendanceMapper extends BaseMapper<Attendance> {
}
