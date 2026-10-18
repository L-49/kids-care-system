package com.kids.care.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 数据看板统计 Mapper（多表聚合查询）
 */
@Mapper
public interface StatMapper {

    @Select("select count(*) from child where deleted = 0 and status = 1")
    Long countChild();

    @Select("select count(*) from teacher where deleted = 0 and status = 1")
    Long countTeacher();

    @Select("select count(*) from clazz where deleted = 0")
    Long countClazz();

    @Select("select count(*) from notice where deleted = 0")
    Long countNotice();

    @Select("select count(*) from leave_request where deleted = 0 and status = 0")
    Long countPendingLeave();

    @Select("select count(*) from fee where deleted = 0 and pay_status = 0")
    Long countUnpaid();

    @Select("select ifnull(sum(amount), 0) from fee where deleted = 0 and pay_status = 0")
    BigDecimal sumUnpaidAmount();

    @Select("select ifnull(sum(amount), 0) from fee where deleted = 0 and pay_status = 1 and fee_month = #{month}")
    BigDecimal sumPaidByMonth(@Param("month") String month);

    /** 今日考勤分布：status -> 人数 */
    @Select("select status, count(*) as num from attendance "
            + "where deleted = 0 and att_date = curdate() group by status")
    List<Map<String, Object>> attendanceToday();

    /** 近 7 天出勤趋势（幼儿园周末不开园，仅统计周一至周五） */
    @Select("select date_format(att_date, '%m-%d') as day, "
            + "sum(case when status = #{present} then 1 else 0 end) as present, count(*) as total "
            + "from attendance where deleted = 0 and att_date >= date_sub(curdate(), interval 6 day) "
            + "and dayofweek(att_date) not in (1, 7) "
            + "group by att_date order by att_date")
    List<Map<String, Object>> attendanceTrend(@Param("present") String present);

    /** 单个幼儿近 7 天每个工作日的考勤状态（家长端） */
    @Select("select date_format(att_date, '%m-%d') as day, status, check_in_time as checkInTime "
            + "from attendance where deleted = 0 and child_id = #{childId} "
            + "and att_date >= date_sub(curdate(), interval 6 day) "
            + "and dayofweek(att_date) not in (1, 7) "
            + "order by att_date")
    List<Map<String, Object>> attendanceDaysOfChild(@Param("childId") Long childId);

    /** 各班在园人数分布 */
    @Select("select c.name as name, count(ch.id) as value from clazz c "
            + "left join child ch on ch.clazz_id = c.id and ch.deleted = 0 and ch.status = 1 "
            + "where c.deleted = 0 group by c.id, c.name order by c.id")
    List<Map<String, Object>> clazzDistribution();

    /** 待缴费明细（最近 5 条） */
    @Select("select ch.name as childName, f.fee_type as feeType, f.amount as amount, "
            + "f.fee_month as feeMonth from fee f join child ch on ch.id = f.child_id "
            + "where f.deleted = 0 and f.pay_status = 0 order by f.fee_month desc, f.id desc limit 5")
    List<Map<String, Object>> unpaidFeeTop();

    /** 近 6 个月收费趋势 */
    @Select("select fee_month as month, sum(case when pay_status = 1 then amount else 0 end) as paid, "
            + "sum(amount) as total from fee where deleted = 0 "
            + "group by fee_month order by fee_month desc limit 6")
    List<Map<String, Object>> feeTrend();

    /** 各班出勤汇总 */
    @Select("select c.name as clazzName, sum(case when a.status = #{present} then 1 else 0 end) as present, "
            + "count(a.id) as total from attendance a join clazz c on c.id = a.clazz_id "
            + "where a.deleted = 0 group by c.id, c.name order by c.id")
    List<Map<String, Object>> attendanceByClazz(@Param("present") String present);

    /** 今日考勤记录总数 */
    @Select("select count(*) from attendance where deleted = 0 and att_date = curdate()")
    Long countTodayAttendance();

    /** 食材低于预警线条数 */
    @Select("select count(*) from food_stock where deleted = 0 and quantity <= warn_line")
    Long countStockWarn();

    /** 操作日志总数 */
    @Select("select count(*) from sys_log")
    Long countLog();
}
