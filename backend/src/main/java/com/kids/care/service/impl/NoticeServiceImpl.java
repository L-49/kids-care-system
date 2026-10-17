package com.kids.care.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kids.care.common.BizException;
import com.kids.care.entity.Notice;
import com.kids.care.mapper.NoticeMapper;
import com.kids.care.service.INoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知公告 业务实现
 *
 * <p>状态机：草稿(0) → 定时待发(1) → 已发布(2) → 已撤回(3)
 * <ul>
 *   <li>定时待发：由 {@link #publishDueNotices()} 每分钟扫描，到点自动置为已发布</li>
 *   <li>已撤回：只改状态、保留记录，不物理删除（审计要求）</li>
 *   <li>对外可见只有一个口径：{@code status=2 且 publish_time <= 当前时间}</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements INoticeService {

    @Override
    public IPage<Notice> pageQuery(int pageNum, int pageSize, String keyword, String type, Integer status) {
        LambdaQueryWrapper<Notice> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like(Notice::getTitle, keyword);
        }
        if (StringUtils.hasText(type)) {
            qw.eq(Notice::getType, type);
        }
        if (status != null) {
            qw.eq(Notice::getStatus, status);
        } else {
            // 管理端默认不展示已撤回的公告，需要查看时显式传 status=3
            qw.ne(Notice::getStatus, Notice.STATUS_REVOKED);
        }
        qw.orderByDesc(Notice::getPublishTime).orderByDesc(Notice::getId);
        return page(new Page<>(pageNum, pageSize), qw);
    }

    /**
     * 对外可见的公告列表（最近 20 条）。
     * 注意：这里必须同时过滤状态与发布时间，否则定时未到的公告会提前露出。
     */
    @Override
    public List<Notice> listAll() {
        return list(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, Notice.STATUS_PUBLISHED)
                .le(Notice::getPublishTime, LocalDateTime.now())
                .orderByDesc(Notice::getPublishTime)
                .orderByDesc(Notice::getId)
                .last("limit 20"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveNotice(Notice entity) {
        normalize(entity);
        if (!super.save(entity)) {
            throw new BizException("公告保存失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNotice(Notice entity) {
        Notice old = getById(entity.getId());
        if (old == null) {
            throw new BizException("公告不存在或已被删除");
        }
        if (old.getStatus() != null && old.getStatus() == Notice.STATUS_REVOKED) {
            throw new BizException("公告已撤回，不能再修改；如需重新发布请新建公告");
        }
        normalize(entity);
        if (!super.updateById(entity)) {
            throw new BizException("公告修改失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishNow(Long id) {
        Notice old = getById(id);
        if (old == null) {
            throw new BizException("公告不存在或已被删除");
        }
        if (old.getStatus() != null && old.getStatus() == Notice.STATUS_REVOKED) {
            throw new BizException("已撤回的公告不能重新发布，请新建公告");
        }
        if (old.getStatus() != null && old.getStatus() == Notice.STATUS_PUBLISHED) {
            throw new BizException("该公告已是发布状态，无需重复发布");
        }
        Notice update = new Notice();
        update.setId(id);
        update.setStatus(Notice.STATUS_PUBLISHED);
        update.setPublishTime(nowSeconds());
        baseMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long id) {
        Notice old = getById(id);
        if (old == null) {
            throw new BizException("公告不存在或已被删除");
        }
        if (old.getStatus() == null || old.getStatus() != Notice.STATUS_PUBLISHED) {
            throw new BizException("只有已发布的公告可以撤回");
        }
        Notice update = new Notice();
        update.setId(id);
        update.setStatus(Notice.STATUS_REVOKED);
        baseMapper.updateById(update);
    }

    /**
     * 定时发布任务：每分钟扫描一次到点的定时公告。
     * 用 Spring 自带的 @Scheduled 即可，不引入 Quartz（单实例部署，无分布式调度需求）。
     */
    @Scheduled(cron = "0 * * * * ?")
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int publishDueNotices() {
        Notice update = new Notice();
        update.setStatus(Notice.STATUS_PUBLISHED);
        int rows = baseMapper.update(update, new LambdaUpdateWrapper<Notice>()
                .eq(Notice::getStatus, Notice.STATUS_SCHEDULED)
                .le(Notice::getPublishTime, LocalDateTime.now()));
        if (rows > 0) {
            log.info("[公告定时发布] 到点自动发布 {} 条", rows);
        }
        return rows;
    }

    /**
     * 状态与发布时间的统一校验。
     *
     * <p>核心规则：状态决定时间的含义，两者不能互相矛盾——
     * 「定时待发」的时间必须在未来，「已发布」的时间必须在现在或过去。
     */
    private void normalize(Notice entity) {
        LocalDateTime now = nowSeconds();
        Integer status = entity.getStatus();
        if (status == null) {
            // 未传状态时按时间推断：时间在未来视为定时，否则视为立即发布
            status = (entity.getPublishTime() != null && entity.getPublishTime().isAfter(now))
                    ? Notice.STATUS_SCHEDULED : Notice.STATUS_PUBLISHED;
        }
        switch (status) {
            case Notice.STATUS_DRAFT -> {
                if (entity.getPublishTime() == null) {
                    // 数据库 publish_time 为 NOT NULL，草稿也要给个占位时间
                    entity.setPublishTime(now);
                }
            }
            case Notice.STATUS_SCHEDULED -> {
                if (entity.getPublishTime() == null) {
                    throw new BizException("定时发布必须选择发布时间");
                }
                if (!entity.getPublishTime().isAfter(now)) {
                    throw new BizException("定时发布时间必须晚于当前时间");
                }
            }
            case Notice.STATUS_PUBLISHED -> {
                if (entity.getPublishTime() == null) {
                    entity.setPublishTime(now);
                } else if (entity.getPublishTime().isAfter(now)) {
                    throw new BizException("发布时间晚于当前时间，请把状态改为「定时发布」");
                }
            }
            case Notice.STATUS_REVOKED ->
                    throw new BizException("不能直接把公告设为「已撤回」，请对已发布的公告执行撤回操作");
            default -> throw new BizException("公告状态不合法");
        }
        entity.setStatus(status);
    }

    /**
     * 截断到秒的当前时间。
     *
     * <p>为什么必须截断：{@code notice.publish_time} 是 {@code DATETIME}（无小数秒），
     * MySQL 写入时会把 {@code .5} 以上**四舍五入**到下一秒（如 16:11:46.7 → 16:11:47）。
     * 若直接用带纳秒的 now() 写库，紧接着的「已发布且 publish_time <= now()」查询
     * 就可能把刚发布的公告判为"还没到时间"，出现最长 1 秒的不可见窗口。
     */
    private static LocalDateTime nowSeconds() {
        return LocalDateTime.now().withNano(0);
    }
}
