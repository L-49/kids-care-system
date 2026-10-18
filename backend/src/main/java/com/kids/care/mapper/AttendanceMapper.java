package com.kids.care.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kids.care.entity.Attendance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;

/**
 * 考勤记录 Mapper
 */
@Mapper
public interface AttendanceMapper extends BaseMapper<Attendance> {

    /** 查找同幼儿同一天已被软删除的记录（唯一键 uk_att 不含 deleted，重录时需要复活而非新增） */
    @Select("select id from attendance where child_id = #{childId} and att_date = #{attDate} "
            + "and deleted = 1 limit 1")
    Long findDeletedId(@Param("childId") Long childId, @Param("attDate") LocalDate attDate);

    /** 复活软删除记录并覆盖最新内容 */
    @Update("update attendance set deleted = 0, clazz_id = #{clazzId}, status = #{status}, "
            + "check_in_time = #{checkInTime}, remark = #{remark}, create_time = now() where id = #{id}")
    int revive(@Param("id") Long id, @Param("clazzId") Long clazzId, @Param("status") String status,
               @Param("checkInTime") String checkInTime, @Param("remark") String remark);
}
