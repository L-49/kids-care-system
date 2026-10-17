package com.kids.care.controller;

import com.kids.care.common.Result;
import com.kids.care.mapper.StatMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页数据看板接口
 */
@RestController
@RequestMapping("/api/stat")
@RequiredArgsConstructor
public class StatController {

    private final StatMapper statMapper;

    /** 看板汇总数据 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("childCount", statMapper.countChild());
        data.put("teacherCount", statMapper.countTeacher());
        data.put("clazzCount", statMapper.countClazz());
        data.put("noticeCount", statMapper.countNotice());
        data.put("pendingLeave", statMapper.countPendingLeave());
        data.put("unpaidCount", statMapper.countUnpaid());
        data.put("unpaidAmount", statMapper.sumUnpaidAmount());
        data.put("monthIncome", statMapper.sumPaidByMonth(YearMonth.now().toString()));

        // 今日考勤：补齐 4 种状态，便于前端直接渲染
        Map<String, Object> today = new HashMap<>();
        today.put("出勤", 0L);
        today.put("迟到", 0L);
        today.put("请假", 0L);
        today.put("缺勤", 0L);
        for (Map<String, Object> row : statMapper.attendanceToday()) {
            today.put(String.valueOf(row.get("status")), row.get("num"));
        }
        data.put("attendanceToday", today);
        data.put("attendanceTrend", statMapper.attendanceTrend());
        data.put("clazzDistribution", statMapper.clazzDistribution());
        data.put("unpaidFeeTop", statMapper.unpaidFeeTop());
        return Result.ok(data);
    }

    /** 近 7 天出勤趋势（单独接口，便于看板刷新） */
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> trend() {
        return Result.ok(statMapper.attendanceTrend());
    }

    /** 统计报表：收费趋势、班级出勤率、库存预警、日志量 */
    @GetMapping("/report")
    public Result<Map<String, Object>> report() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("feeTrend", statMapper.feeTrend());
        data.put("attendanceByClazz", statMapper.attendanceByClazz());
        data.put("attendanceToday", statMapper.attendanceToday());
        data.put("clazzDistribution", statMapper.clazzDistribution());
        data.put("childCount", statMapper.countChild());
        data.put("todayTotal", statMapper.countTodayAttendance());
        data.put("stockWarn", statMapper.countStockWarn());
        data.put("logCount", statMapper.countLog());
        data.put("unpaidAmount", statMapper.sumUnpaidAmount());
        data.put("unpaidCount", statMapper.countUnpaid());
        data.put("monthIncome", statMapper.sumPaidByMonth(YearMonth.now().toString()));
        return Result.ok(data);
    }
}
