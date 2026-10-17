package com.kids.care.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kids.care.entity.LeaveRequest;
import com.kids.care.mapper.LeaveRequestMapper;
import com.kids.care.service.ILeaveRequestService;
import com.kids.care.common.BizException;
import com.kids.care.entity.Child;
import com.kids.care.mapper.ChildMapper;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 请假申请 业务实现
 */
@Service
@RequiredArgsConstructor
public class LeaveRequestServiceImpl extends ServiceImpl<LeaveRequestMapper, LeaveRequest> implements ILeaveRequestService {

    private final ChildMapper childMapper;
    @Override
    public IPage<LeaveRequest> pageQuery(int pageNum, int pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<LeaveRequest> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            List<Long> childIds = childMapper.selectList(new LambdaQueryWrapper<Child>()
                            .select(Child::getId).like(Child::getName, keyword))
                    .stream().map(Child::getId).toList();
            if (childIds.isEmpty()) {
                return new Page<>(pageNum, pageSize);
            }
            qw.in(LeaveRequest::getChildId, childIds);
        }
        if (status != null) {
            qw.eq(LeaveRequest::getStatus, status);
        }
        qw.orderByAsc(LeaveRequest::getStatus).orderByDesc(LeaveRequest::getId);
        IPage<LeaveRequest> page = page(new Page<>(pageNum, pageSize), qw);
        fillChildName(page.getRecords());
        return page;
    }

    @Override
    public List<LeaveRequest> listAll() {
        List<LeaveRequest> list = list(new LambdaQueryWrapper<LeaveRequest>()
                .orderByDesc(LeaveRequest::getId).last("limit 100"));
        fillChildName(list);
        return list;
    }

    /** 新增请假单：天数由起止日期自动计算 */
    @Override
    public boolean save(LeaveRequest entity) {
        if (entity.getStartDate() != null && entity.getEndDate() != null) {
            long between = ChronoUnit.DAYS.between(entity.getStartDate(), entity.getEndDate()) + 1;
            entity.setDays((int) between);
        }
        entity.setStatus(0);
        return super.save(entity);
    }

    /** 审批：pass=true 通过，false 驳回 */
    @Override
    public boolean approve(Long id, boolean pass, String approver) {
        LeaveRequest req = getById(id);
        if (req == null) {
            throw new BizException("请假单不存在");
        }
        if (req.getStatus() != null && req.getStatus() != 0) {
            throw new BizException("该请假单已审批，请勿重复操作");
        }
        req.setStatus(pass ? 1 : 2);
        req.setApprover(approver);
        req.setApproveTime(LocalDateTime.now());
        return updateById(req);
    }

    private void fillChildName(List<LeaveRequest> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> childIds = list.stream().map(LeaveRequest::getChildId).filter(Objects::nonNull).distinct().toList();
        Map<Long, Child> childMap = childIds.isEmpty() ? Map.of()
                : childMapper.selectBatchIds(childIds).stream()
                    .collect(Collectors.toMap(Child::getId, Function.identity()));
        list.forEach(l -> {
            Child child = childMap.get(l.getChildId());
            if (child != null) {
                l.setChildName(child.getName());
                l.setClazzName(child.getClazzName());
            }
        });
    }
}
