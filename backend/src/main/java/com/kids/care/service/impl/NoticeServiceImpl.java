package com.kids.care.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kids.care.entity.Notice;
import com.kids.care.mapper.NoticeMapper;
import com.kids.care.service.INoticeService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 通知公告 业务实现
 */
@Service
@RequiredArgsConstructor
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements INoticeService {

    @Override
    public IPage<Notice> pageQuery(int pageNum, int pageSize, String keyword, String type) {
        LambdaQueryWrapper<Notice> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like(Notice::getTitle, keyword);
        }
        if (StringUtils.hasText(type)) {
            qw.eq(Notice::getType, type);
        }
        qw.orderByDesc(Notice::getPublishTime).orderByDesc(Notice::getId);
        return page(new Page<>(pageNum, pageSize), qw);
    }

    @Override
    public List<Notice> listAll() {
        return list(new LambdaQueryWrapper<Notice>().orderByDesc(Notice::getPublishTime).last("limit 20"));
    }

    /** 发布时若未指定发布时间则取当前时间 */
    @Override
    public boolean save(Notice entity) {
        if (entity.getPublishTime() == null) {
            entity.setPublishTime(LocalDateTime.now());
        }
        return super.save(entity);
    }
}
