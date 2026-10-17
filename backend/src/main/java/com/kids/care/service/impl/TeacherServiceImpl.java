package com.kids.care.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kids.care.entity.Teacher;
import com.kids.care.mapper.TeacherMapper;
import com.kids.care.service.ITeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 教职工 业务实现
 */
@Service
@RequiredArgsConstructor
public class TeacherServiceImpl extends ServiceImpl<TeacherMapper, Teacher> implements ITeacherService {

    @Override
    public IPage<Teacher> pageQuery(int pageNum, int pageSize, String keyword, String role, Integer status) {
        LambdaQueryWrapper<Teacher> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Teacher::getName, keyword).or().like(Teacher::getPhone, keyword));
        }
        if (StringUtils.hasText(role)) {
            qw.eq(Teacher::getRole, role);
        }
        if (status != null) {
            qw.eq(Teacher::getStatus, status);
        }
        qw.orderByAsc(Teacher::getId);
        return page(new Page<>(pageNum, pageSize), qw);
    }

    @Override
    public List<Teacher> listAll() {
        return list(new LambdaQueryWrapper<Teacher>().eq(Teacher::getStatus, 1).orderByAsc(Teacher::getId));
    }
}
