package com.kids.care.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kids.care.common.BizException;
import com.kids.care.common.ChildCompleteness;
import com.kids.care.entity.Child;
import com.kids.care.mapper.ChildMapper;
import com.kids.care.service.IChildService;
import com.kids.care.entity.Clazz;
import com.kids.care.mapper.ClazzMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 幼儿档案 业务实现
 */
@Service
@RequiredArgsConstructor
public class ChildServiceImpl extends ServiceImpl<ChildMapper, Child> implements IChildService {

    private final ClazzMapper clazzMapper;
    @Override
    public IPage<Child> pageQuery(int pageNum, int pageSize, String keyword, Long clazzId,
                                  Integer status, Boolean incompleteOnly) {
        LambdaQueryWrapper<Child> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Child::getName, keyword)
                    .or().like(Child::getGuardianName, keyword)
                    .or().like(Child::getGuardianPhone, keyword));
        }
        if (clazzId != null) {
            qw.eq(Child::getClazzId, clazzId);
        }
        if (status != null) {
            qw.eq(Child::getStatus, status);
        }
        if (Boolean.TRUE.equals(incompleteOnly)) {
            // 用 SQL 侧算完整度，保证分页 total 与筛选结果一致（在 Java 里过滤会导致条数错乱）
            qw.apply(ChildCompleteness.incompleteSql());
        }
        qw.orderByAsc(Child::getClazzId).orderByAsc(Child::getId);
        IPage<Child> page = page(new Page<>(pageNum, pageSize), qw);
        fillClazzName(page.getRecords());
        fillCompleteness(page.getRecords());
        return page;
    }

    @Override
    public List<Child> listAll() {
        List<Child> list = list(new LambdaQueryWrapper<Child>()
                .eq(Child::getStatus, 1).orderByAsc(Child::getClazzId).orderByAsc(Child::getId));
        fillClazzName(list);
        return list;
    }

    @Override
    public List<Child> listAllergy() {
        List<Child> list = list(new LambdaQueryWrapper<Child>()
                .eq(Child::getStatus, 1)
                .isNotNull(Child::getAllergy)
                .ne(Child::getAllergy, "")
                .orderByAsc(Child::getClazzId));
        fillClazzName(list);
        return list;
    }

    private void fillClazzName(List<Child> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> ids = list.stream().map(Child::getClazzId).filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, String> names = clazzMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Clazz::getId, Clazz::getName));
        list.forEach(c -> c.setClazzName(names.get(c.getClazzId())));
    }

    /** 填充档案完整度（实时计算，不落库） */
    private void fillCompleteness(List<Child> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (Child c : list) {
            c.setCompleteness(ChildCompleteness.score(c));
            c.setMissingFields(ChildCompleteness.missingLabels(c));
        }
    }

    @Override
    public void saveChild(Child entity) {
        normalize(entity);
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        save(entity);
    }

    @Override
    public void updateChild(Child entity) {
        if (entity.getId() == null) {
            throw new BizException("缺少幼儿编号，无法修改");
        }
        if (getById(entity.getId()) == null) {
            throw new BizException("该幼儿档案不存在或已删除");
        }
        normalize(entity);
        updateById(entity);
    }

    /** 统一整理入参：去空格、补默认性别、校验班级存在 */
    private void normalize(Child entity) {
        if (StringUtils.hasText(entity.getName())) {
            entity.setName(entity.getName().trim());
        }
        if (!StringUtils.hasText(entity.getGender())) {
            entity.setGender("男");
        }
        if (entity.getClazzId() != null && clazzMapper.selectById(entity.getClazzId()) == null) {
            throw new BizException("所选班级不存在，请重新选择");
        }
    }
}
