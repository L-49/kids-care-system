package com.kids.care.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kids.care.common.BizException;
import com.kids.care.entity.Course;
import com.kids.care.mapper.CourseMapper;
import com.kids.care.service.ICourseService;
import com.kids.care.entity.Clazz;
import com.kids.care.entity.Teacher;
import com.kids.care.mapper.ClazzMapper;
import com.kids.care.mapper.TeacherMapper;
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
 * 课程活动 业务实现
 */
@Service
@RequiredArgsConstructor
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements ICourseService {

    private final ClazzMapper clazzMapper;
    private final TeacherMapper teacherMapper;

    @Override
    public boolean save(Course entity) {
        checkConflict(entity);
        return super.save(entity);
    }

    @Override
    public boolean updateById(Course entity) {
        checkConflict(entity);
        return super.updateById(entity);
    }

    /** 同一教师同一天的时间段不允许重叠（修改时排除自身） */
    private void checkConflict(Course entity) {
        if (entity.getTeacherId() == null || entity.getCourseDate() == null
                || !StringUtils.hasText(entity.getStartTime()) || !StringUtils.hasText(entity.getEndTime())) {
            return;
        }
        if (entity.getStartTime().compareTo(entity.getEndTime()) >= 0) {
            throw new BizException("开始时间不能晚于或等于结束时间");
        }
        List<Course> sameDay = list(new LambdaQueryWrapper<Course>()
                .eq(Course::getTeacherId, entity.getTeacherId())
                .eq(Course::getCourseDate, entity.getCourseDate())
                .ne(entity.getId() != null, Course::getId, entity.getId()));
        for (Course other : sameDay) {
            if (!StringUtils.hasText(other.getStartTime()) || !StringUtils.hasText(other.getEndTime())) {
                continue;
            }
            boolean overlap = entity.getStartTime().compareTo(other.getEndTime()) < 0
                    && other.getStartTime().compareTo(entity.getEndTime()) < 0;
            if (overlap) {
                throw new BizException("该教师在 " + entity.getCourseDate() + " "
                        + other.getStartTime() + "~" + other.getEndTime()
                        + " 已安排课程【" + other.getTitle() + "】，时间冲突");
            }
        }
    }

    @Override
    public IPage<Course> pageQuery(int pageNum, int pageSize, String keyword, Long clazzId,
                                   Long teacherId, String courseDate) {
        LambdaQueryWrapper<Course> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like(Course::getTitle, keyword);
        }
        if (clazzId != null) {
            qw.eq(Course::getClazzId, clazzId);
        }
        if (teacherId != null) {
            qw.eq(Course::getTeacherId, teacherId);
        }
        if (StringUtils.hasText(courseDate)) {
            qw.eq(Course::getCourseDate, LocalDate.parse(courseDate));
        }
        qw.orderByDesc(Course::getCourseDate).orderByAsc(Course::getStartTime);
        IPage<Course> page = page(new Page<>(pageNum, pageSize), qw);
        fillName(page.getRecords());
        return page;
    }

    @Override
    public List<Course> listAll() {
        List<Course> list = list(new LambdaQueryWrapper<Course>()
                .orderByDesc(Course::getCourseDate).last("limit 100"));
        fillName(list);
        return list;
    }

    private void fillName(List<Course> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> clazzIds = list.stream().map(Course::getClazzId).filter(Objects::nonNull).distinct().toList();
        List<Long> teacherIds = list.stream().map(Course::getTeacherId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> clazzNames = clazzIds.isEmpty() ? Map.of()
                : clazzMapper.selectBatchIds(clazzIds).stream()
                    .collect(Collectors.toMap(Clazz::getId, Clazz::getName));
        Map<Long, String> teacherNames = teacherIds.isEmpty() ? Map.of()
                : teacherMapper.selectBatchIds(teacherIds).stream()
                    .collect(Collectors.toMap(Teacher::getId, Teacher::getName));
        list.forEach(c -> {
            c.setClazzName(clazzNames.get(c.getClazzId()));
            c.setTeacherName(teacherNames.get(c.getTeacherId()));
        });
    }
}
