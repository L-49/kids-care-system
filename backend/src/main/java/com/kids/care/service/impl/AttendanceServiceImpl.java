package com.kids.care.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kids.care.common.AttendanceStatus;
import com.kids.care.common.BizException;
import com.kids.care.entity.Attendance;
import com.kids.care.entity.Child;
import com.kids.care.entity.Clazz;
import com.kids.care.mapper.AttendanceMapper;
import com.kids.care.mapper.ChildMapper;
import com.kids.care.mapper.ClazzMapper;
import com.kids.care.service.IAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 考勤记录 业务实现
 */
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl extends ServiceImpl<AttendanceMapper, Attendance> implements IAttendanceService {

    private final ChildMapper childMapper;
    private final ClazzMapper clazzMapper;

    @Override
    public IPage<Attendance> pageQuery(int pageNum, int pageSize, String keyword, Long clazzId,
                                       String status, String attDate, Long childId) {
        LambdaQueryWrapper<Attendance> qw = new LambdaQueryWrapper<>();
        if (childId != null) {
            // 家长端：只允许查自己孩子，忽略其他筛选条件
            qw.eq(Attendance::getChildId, childId);
        } else if (StringUtils.hasText(keyword)) {
            List<Long> childIds = childMapper.selectList(new LambdaQueryWrapper<Child>()
                            .select(Child::getId).like(Child::getName, keyword))
                    .stream().map(Child::getId).toList();
            if (childIds.isEmpty()) {
                return new Page<>(pageNum, pageSize);
            }
            qw.in(Attendance::getChildId, childIds);
        }
        if (childId == null && clazzId != null) {
            qw.eq(Attendance::getClazzId, clazzId);
        }
        if (StringUtils.hasText(status)) {
            qw.eq(Attendance::getStatus, status);
        }
        if (StringUtils.hasText(attDate)) {
            qw.eq(Attendance::getAttDate, LocalDate.parse(attDate));
        }
        qw.orderByDesc(Attendance::getAttDate).orderByAsc(Attendance::getChildId);
        IPage<Attendance> page = page(new Page<>(pageNum, pageSize), qw);
        fillName(page.getRecords());
        return page;
    }

    @Override
    public List<Attendance> listAll() {
        List<Attendance> list = list(new LambdaQueryWrapper<Attendance>()
                .orderByDesc(Attendance::getAttDate).last("limit 200"));
        fillName(list);
        return list;
    }

    /**
     * 新增考勤：班级以幼儿档案为准；同一天已有记录时给出友好提示；
     * 若该记录只是被软删除过，则复活旧记录（唯一键 uk_att 不含 deleted）。
     */
    @Override
    public boolean save(Attendance entity) {
        if (entity.getChildId() == null) {
            throw new BizException("请选择幼儿");
        }
        if (!AttendanceStatus.isValid(entity.getStatus())) {
            throw new BizException("考勤状态只能是出勤、迟到、请假、缺勤");
        }
        if (entity.getAttDate() == null) {
            entity.setAttDate(LocalDate.now());
        }
        Child child = childMapper.selectById(entity.getChildId());
        if (child == null) {
            throw new BizException("幼儿档案不存在或已删除");
        }
        entity.setClazzId(child.getClazzId());

        boolean exists = count(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getChildId, entity.getChildId())
                .eq(Attendance::getAttDate, entity.getAttDate())) > 0;
        if (exists) {
            throw new BizException("该幼儿当天已有考勤记录，请直接编辑原记录");
        }
        Long deletedId = baseMapper.findDeletedId(entity.getChildId(), entity.getAttDate());
        if (deletedId != null) {
            return baseMapper.revive(deletedId, entity.getClazzId(), entity.getStatus(),
                    entity.getCheckInTime(), entity.getRemark()) > 0;
        }
        return super.save(entity);
    }

    @Override
    public boolean updateById(Attendance entity) {
        if (entity.getStatus() != null && !AttendanceStatus.isValid(entity.getStatus())) {
            throw new BizException("考勤状态只能是出勤、迟到、请假、缺勤");
        }
        if (entity.getChildId() != null) {
            Child child = childMapper.selectById(entity.getChildId());
            if (child != null) {
                entity.setClazzId(child.getClazzId());
            }
        }
        return super.updateById(entity);
    }

    /** 补齐幼儿姓名与班级名（班级名按幼儿真实班级查询，不能依赖 Child.clazzName） */
    private void fillName(List<Attendance> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> childIds = list.stream().map(Attendance::getChildId).filter(Objects::nonNull).distinct().toList();
        if (childIds.isEmpty()) {
            return;
        }
        List<Child> children = childMapper.selectBatchIds(childIds);
        Map<Long, Child> childMap = children.stream()
                .collect(Collectors.toMap(Child::getId, Function.identity()));
        List<Long> clazzIds = children.stream().map(Child::getClazzId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> clazzNames = clazzIds.isEmpty() ? Map.of()
                : clazzMapper.selectBatchIds(clazzIds).stream()
                    .collect(Collectors.toMap(Clazz::getId, Clazz::getName));
        list.forEach(a -> {
            Child child = childMap.get(a.getChildId());
            if (child != null) {
                a.setChildName(child.getName());
                a.setClazzName(clazzNames.get(child.getClazzId()));
            }
        });
    }
}
