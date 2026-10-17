package com.kids.care.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kids.care.entity.SysLog;
import com.kids.care.mapper.SysLogMapper;
import com.kids.care.service.ISysLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 操作日志 业务实现
 */
@Service
@RequiredArgsConstructor
public class SysLogServiceImpl extends ServiceImpl<SysLogMapper, SysLog> implements ISysLogService {

    @Override
    public IPage<SysLog> pageQuery(int pageNum, int pageSize, String keyword, String method) {
        LambdaQueryWrapper<SysLog> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(SysLog::getOperation, keyword).or().like(SysLog::getUsername, keyword));
        }
        if (StringUtils.hasText(method)) {
            qw.eq(SysLog::getMethod, method);
        }
        qw.orderByDesc(SysLog::getId);
        return page(new Page<>(pageNum, pageSize), qw);
    }

    @Override
    public List<SysLog> listAll() {
        return list(new LambdaQueryWrapper<SysLog>().orderByDesc(SysLog::getId).last("limit 50"));
    }

    @Override
    public void clean() {
        baseMapper.truncate();
    }
}
