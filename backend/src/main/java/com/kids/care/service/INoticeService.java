package com.kids.care.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kids.care.entity.Notice;

import java.util.List;

/**
 * 通知公告 业务接口
 */
public interface INoticeService extends IService<Notice> {

    /** 分页查询（status 为空时默认不展示已撤回） */
    IPage<Notice> pageQuery(int pageNum, int pageSize, String keyword, String type, Integer status);

    /** 全部列表（用于下拉框）：只返回已发布且已到发布时间的公告 */
    List<Notice> listAll();

    /**
     * 详情（带可见性校验）。
     * 家长只能看到「已发布 + 已到点 + 在可见范围内」的公告，其余按不存在处理；管理端原样返回。
     */
    Notice visibleDetail(Long id);

    /** 新增公告（按状态校验并补全发布时间） */
    void saveNotice(Notice entity);

    /** 修改公告（按状态校验并补全发布时间） */
    void updateNotice(Notice entity);

    /** 立即发布：草稿 / 定时公告立刻变为已发布 */
    void publishNow(Long id);

    /** 撤回：已发布公告变为已撤回（保留记录，不物理删除） */
    void revoke(Long id);

    /** 定时任务调用：把到点的定时公告置为已发布，返回处理条数 */
    int publishDueNotices();
}
