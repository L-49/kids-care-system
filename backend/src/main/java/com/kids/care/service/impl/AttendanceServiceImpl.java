package com.kids.care.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kids.care.entity.Attendance;
import com.kids.care.mapper.AttendanceMapper;
import com.kids.care.service.IAttendanceService;
import com.kids.care.entity.Child;
import com.kids.care.mapper.ChildMapper;
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
    @Override
    public IPage<Attendance> pageQuery(int pageNum, int pageSize, String keyword, Long clazzId,
                                       String status, String attDate) {
        LambdaQueryWrapper<Attendance> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            List<Long> childIds = childMapper.selectList(new LambdaQueryWrapper<Child>()
                            .select(Child::getId).like(Child::getName, keyword))
                    .stream().map(Child::getId).toList();
            if (childIds.isEmpty()) {
                return new Page<>(pageNum, pageSize);
            }
            qw.in(Attendance::getChildId, childIds);
        }
        if (clazzId != null) {
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

    /** 新增时自动补全班级ID，避免前端重复传参 */
    @Override
    public boolean save(Attendance entity) {
        if (entity.getClazzId() == null && entity.getChildId() != null) {
            Child child = childMapper.selectById(entity.getChildId());
            if (child != null) {
                entity.setClazzId(child.getClazzId());
            }
        }
        return super.save(entity);
    }

    private void fillName(List<Attendance> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> childIds = list.stream().map(Attendance::getChildId).filter(Objects::nonNull).distinct().toList();
        Map<Long, Child> childMap = childIds.isEmpty() ? Map.of()
                : childMapper.selectBatchIds(childIds).stream()
                    .collect(Collectors.toMap(Child::getId, Function.identity()));
        list.forEach(a -> {
            Child child = childMap.get(a.getChildId());
            if (child != null) {
                a.setChildName(child.getName());
                a.setClazzName(child.getClazzName());
            }
        });
    }
}
