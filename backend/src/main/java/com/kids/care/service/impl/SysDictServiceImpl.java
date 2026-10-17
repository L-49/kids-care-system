package com.kids.care.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kids.care.entity.SysDict;
import com.kids.care.mapper.SysDictMapper;
import com.kids.care.service.ISysDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 数据字典 业务实现
 */
@Service
@RequiredArgsConstructor
public class SysDictServiceImpl extends ServiceImpl<SysDictMapper, SysDict> implements ISysDictService {

    @Override
    public IPage<SysDict> pageQuery(int pageNum, int pageSize, String keyword, String dictType) {
        LambdaQueryWrapper<SysDict> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(SysDict::getDictLabel, keyword).or().like(SysDict::getDictValue, keyword));
        }
        if (StringUtils.hasText(dictType)) {
            qw.eq(SysDict::getDictType, dictType);
        }
        qw.orderByAsc(SysDict::getDictType).orderByAsc(SysDict::getSort);
        return page(new Page<>(pageNum, pageSize), qw);
    }

    @Override
    public List<SysDict> listAll() {
        return list(new LambdaQueryWrapper<SysDict>().eq(SysDict::getStatus, 1)
                .orderByAsc(SysDict::getDictType).orderByAsc(SysDict::getSort));
    }

    @Override
    public List<SysDict> listByType(String dictType) {
        return list(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getStatus, 1)
                .eq(SysDict::getDictType, dictType)
                .orderByAsc(SysDict::getSort));
    }
}
