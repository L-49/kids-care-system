package com.kids.care.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kids.care.entity.Notice;

import java.util.List;

/**
 * 通知公告 业务接口
 */
public interface INoticeService extends IService<Notice> {

    /** 分页查询 */
    IPage<Notice> pageQuery(int pageNum, int pageSize, String keyword, String type);

    /** 全部列表（用于下拉框） */
    List<Notice> listAll();
}
