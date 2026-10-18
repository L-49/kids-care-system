package com.kids.care.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kids.care.common.BizException;
import com.kids.care.entity.Child;
import com.kids.care.entity.Clazz;
import com.kids.care.entity.Teacher;
import com.kids.care.mapper.ChildMapper;
import com.kids.care.mapper.ClazzMapper;
import com.kids.care.mapper.TeacherMapper;
import com.kids.care.service.IClazzService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 班级 业务实现
 */
@Service
@RequiredArgsConstructor
public class ClazzServiceImpl extends ServiceImpl<ClazzMapper, Clazz> implements IClazzService {

    private final TeacherMapper teacherMapper;
    private final ChildMapper childMapper;

    @Override
    public IPage<Clazz> pageQuery(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<Clazz> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like(Clazz::getName, keyword);
        }
        qw.orderByAsc(Clazz::getId);
        IPage<Clazz> page = page(new Page<>(pageNum, pageSize), qw);
        fill(page.getRecords());
        return page;
    }

    @Override
    public List<Clazz> listAll() {
        List<Clazz> list = list(new LambdaQueryWrapper<Clazz>().orderByAsc(Clazz::getId));
        fill(list);
        return list;
    }

    /** 新增班级时校验班主任唯一性 */
    @Override
    public boolean save(Clazz entity) {
        checkHeadTeacher(null, entity.getHeadTeacherId());
        return super.save(entity);
    }

    /** 修改班级时不允许把容量调到低于在读人数，且班主任不能重复 */
    @Override
    public boolean updateById(Clazz entity) {
        checkHeadTeacher(entity.getId(), entity.getHeadTeacherId());
        if (entity.getId() != null && entity.getCapacity() != null) {
            long current = childCountOf(entity.getId());
            if (entity.getCapacity() < current) {
                throw new BizException("该班现有 " + current + " 名幼儿，容量不能低于在读人数");
            }
        }
        return super.updateById(entity);
    }

    /** 班主任唯一性校验：同一教职工不能同时担任多个班级的班主任 */
    private void checkHeadTeacher(Long selfId, Long headTeacherId) {
        if (headTeacherId == null) {
            return;
        }
        LambdaQueryWrapper<Clazz> qw = new LambdaQueryWrapper<>();
        qw.eq(Clazz::getHeadTeacherId, headTeacherId);
        if (selfId != null) {
            qw.ne(Clazz::getId, selfId);
        }
        if (count(qw) > 0) {
            Teacher t = teacherMapper.selectById(headTeacherId);
            throw new BizException("教职工【" + (t == null ? headTeacherId : t.getName())
                    + "】已担任其他班级的班主任，不可重复设置");
        }
    }

    /** 班级下仍有幼儿时禁止删除 */
    @Override
    public boolean removeByIds(Collection<?> list) {
        for (Object id : list) {
            long current = childCountOf(Long.valueOf(String.valueOf(id)));
            if (current > 0) {
                Clazz clazz = getById(Long.valueOf(String.valueOf(id)));
                throw new BizException("班级【" + (clazz == null ? id : clazz.getName())
                        + "】还有 " + current + " 名幼儿，请先调班后再删除");
            }
        }
        return super.removeByIds(list);
    }

    /** 补齐班主任姓名与在园人数（人数用一次 group by 聚合，避免全表扫描） */
    private void fill(List<Clazz> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> teacherIds = list.stream().map(Clazz::getHeadTeacherId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> teacherNames = teacherIds.isEmpty() ? Map.of()
                : teacherMapper.selectBatchIds(teacherIds).stream()
                    .collect(Collectors.toMap(Teacher::getId, Teacher::getName));
        Map<Long, Long> counts = childMapper.countByClazz().stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row.get("clazzId")).longValue(),
                        row -> ((Number) row.get("num")).longValue()));
        list.forEach(c -> {
            c.setHeadTeacherName(teacherNames.get(c.getHeadTeacherId()));
            long cnt = counts.getOrDefault(c.getId(), 0L);
            c.setChildCount(cnt);
            // 满员状态：0正常 1满员 2超载（按在园人数与额定容量比较）
            if (c.getCapacity() != null) {
                if (cnt > c.getCapacity()) {
                    c.setFullStatus(2);
                } else if (cnt == c.getCapacity()) {
                    c.setFullStatus(1);
                } else {
                    c.setFullStatus(0);
                }
            } else {
                c.setFullStatus(0);
            }
        });
    }

    private long childCountOf(Long clazzId) {
        return childMapper.selectCount(new LambdaQueryWrapper<Child>()
                .eq(Child::getClazzId, clazzId)
                .eq(Child::getStatus, 1));
    }
}
