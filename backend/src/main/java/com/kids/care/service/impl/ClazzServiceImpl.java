package com.kids.care.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kids.care.entity.Clazz;
import com.kids.care.mapper.ClazzMapper;
import com.kids.care.service.IClazzService;
import com.kids.care.entity.Child;
import com.kids.care.entity.Teacher;
import com.kids.care.mapper.ChildMapper;
import com.kids.care.mapper.TeacherMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
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

    /** 补齐班主任姓名与在园人数 */
    private void fill(List<Clazz> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> teacherIds = list.stream().map(Clazz::getHeadTeacherId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> teacherNames = teacherIds.isEmpty() ? Map.of()
                : teacherMapper.selectBatchIds(teacherIds).stream()
                    .collect(Collectors.toMap(Teacher::getId, Teacher::getName));
        List<Child> children = childMapper.selectList(
                new LambdaQueryWrapper<Child>().select(Child::getId, Child::getClazzId));
        Map<Long, Long> counts = children.stream().filter(c -> c.getClazzId() != null)
                .collect(Collectors.groupingBy(Child::getClazzId, Collectors.counting()));
        list.forEach(c -> {
            c.setHeadTeacherName(teacherNames.get(c.getHeadTeacherId()));
            c.setChildCount(counts.getOrDefault(c.getId(), 0L));
        });
    }
}
