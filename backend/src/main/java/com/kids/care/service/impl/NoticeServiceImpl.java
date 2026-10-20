package com.kids.care.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kids.care.common.BizException;
import com.kids.care.common.UserContext;
import com.kids.care.entity.Child;
import com.kids.care.entity.Clazz;
import com.kids.care.entity.Notice;
import com.kids.care.entity.SysUser;
import com.kids.care.mapper.ChildMapper;
import com.kids.care.mapper.ClazzMapper;
import com.kids.care.mapper.NoticeMapper;
import com.kids.care.mapper.SysUserMapper;
import com.kids.care.service.INoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通知公告 业务实现
 *
 * <p>状态机：草稿(0) → 定时待发(1) → 已发布(2) → 已撤回(3)
 * <ul>
 *   <li>定时待发：由 {@link #publishDueNotices()} 每分钟扫描，到点自动置为已发布</li>
 *   <li>已撤回：只改状态、保留记录，不物理删除（审计要求）</li>
 *   <li>对外可见口径：{@code status=2 且 publish_time <= 当前时间}</li>
 * </ul>
 *
 * <p>定向可见只做两档：全园 / 指定班级。
 * 家长的班级来自 {@code child.guardian_phone = sys_user.phone} 的关联
 * （种子数据里 parent 张先生 13800000003 正好是张小明 的家长电话）。
 * <b>教师维度暂不做</b>——sys_user 与 teacher 表没有关联字段，绑不上。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements INoticeService {

    private static final String ROLE_PARENT = "parent";

    private final SysUserMapper sysUserMapper;
    private final ChildMapper childMapper;
    private final ClazzMapper clazzMapper;

    @Override
    public IPage<Notice> pageQuery(int pageNum, int pageSize, String keyword, String type, Integer status) {
        LambdaQueryWrapper<Notice> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like(Notice::getTitle, keyword);
        }
        if (StringUtils.hasText(type)) {
            qw.eq(Notice::getType, type);
        }
        if (isParent()) {
            // 家长端：状态与发布时间由服务端判定，忽略前端传入的 status。
            // 否则传 status=0 就能把草稿翻出来，传 status=1 能看到定时未到点的公告。
            applyPublicVisibility(qw);
        } else {
            if (status != null) {
                qw.eq(Notice::getStatus, status);
            } else {
                // 管理端默认不展示已撤回的公告，需要查看时显式传 status=3
                qw.ne(Notice::getStatus, Notice.STATUS_REVOKED);
            }
            applyVisibility(qw);
        }
        qw.orderByDesc(Notice::getPublishTime).orderByDesc(Notice::getId);
        IPage<Notice> page = page(new Page<>(pageNum, pageSize), qw);
        fillTargetLabel(page.getRecords());
        return page;
    }

    /**
     * 对外可见的公告列表（最近 20 条）。
     * 必须同时过滤状态与发布时间，否则定时未到的公告会提前露出。
     */
    @Override
    public List<Notice> listAll() {
        LambdaQueryWrapper<Notice> qw = new LambdaQueryWrapper<>();
        applyPublicVisibility(qw);
        qw.orderByDesc(Notice::getPublishTime).orderByDesc(Notice::getId).last("limit 20");
        List<Notice> list = list(qw);
        fillTargetLabel(list);
        return list;
    }

    @Override
    public Notice visibleDetail(Long id) {
        Notice notice = getById(id);
        if (notice == null) {
            throw new BizException("公告不存在或已删除");
        }
        if (!isParent()) {
            return notice;
        }
        // 家长：草稿、定时未到点、超出可见范围一律按「不存在」处理，
        // 不透露公告真实状态，避免通过遍历 id 提前看到未公开内容
        boolean due = notice.getPublishTime() != null
                && !notice.getPublishTime().isAfter(LocalDateTime.now());
        if (notice.getStatus() == null || notice.getStatus() != Notice.STATUS_PUBLISHED
                || !due || !inParentScope(notice)) {
            throw new BizException("公告不存在或已下线");
        }
        return notice;
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
        // 老接口调用方可能不传可见范围，此时沿用原值，避免误改成全园
        if (entity.getTargetType() == null) {
            entity.setTargetType(old.getTargetType());
            entity.setTargetId(old.getTargetId());
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

    // ==================== 定向可见 ====================

    private boolean isParent() {
        return ROLE_PARENT.equals(UserContext.role());
    }

    /**
     * 对外可见口径（家长端专用）：已发布 + 发布时间已到 + 可见范围匹配。
     *
     * <p>状态与时间必须同时过滤，否则草稿和定时未到点的公告会提前露出。
     * 管理端不能用这个口径——园长/教师需要看草稿与定时待发。
     */
    private void applyPublicVisibility(LambdaQueryWrapper<Notice> qw) {
        qw.eq(Notice::getStatus, Notice.STATUS_PUBLISHED)
                .le(Notice::getPublishTime, LocalDateTime.now());
        applyVisibility(qw);
    }

    /**
     * 只追加「可见范围」条件，状态与发布时间由调用方自行处理。
     *
     * <p>只有家长受限（全园 + 自己孩子所在班级）；园长与教师视为管理视角，不加范围限制。
     * 家长端还没有独立界面，所以这里对管理端列表也生效——方便演示：
     * 用 parent 账号登录就能直接看到「小班专属通知」不会出现在中班家长面前。
     */
    private void applyVisibility(LambdaQueryWrapper<Notice> qw) {
        if (!isParent()) {
            return;
        }
        Set<Long> clazzIds = parentClazzIds();
        if (clazzIds.isEmpty()) {
            qw.eq(Notice::getTargetType, Notice.TARGET_ALL);
            return;
        }
        qw.and(w -> w.eq(Notice::getTargetType, Notice.TARGET_ALL)
                .or(o -> o.eq(Notice::getTargetType, Notice.TARGET_CLASS)
                        .in(Notice::getTargetId, clazzIds)));
    }

    /** 单条公告是否落在家长可见范围内（全园，或自己孩子所在班级） */
    private boolean inParentScope(Notice notice) {
        if (Notice.TARGET_ALL.equals(notice.getTargetType())) {
            return true;
        }
        return Notice.TARGET_CLASS.equals(notice.getTargetType())
                && notice.getTargetId() != null
                && parentClazzIds().contains(notice.getTargetId());
    }

    /** 家长账号 → 手机号 → 其孩子所在班级（无绑定表，用电话关联） */
    private Set<Long> parentClazzIds() {
        Long uid = UserContext.userId();
        if (uid == null) {
            return Set.of();
        }
        SysUser user = sysUserMapper.selectById(uid);
        if (user == null || !StringUtils.hasText(user.getPhone())) {
            return Set.of();
        }
        return childMapper.selectList(new LambdaQueryWrapper<Child>()
                        .eq(Child::getGuardianPhone, user.getPhone()))
                .stream().map(Child::getClazzId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /** 把 target_id 翻译成班级名，列表直接展示「小班」而不是「2」 */
    private void fillTargetLabel(List<Notice> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<Long> ids = list.stream()
                .filter(n -> Notice.TARGET_CLASS.equals(n.getTargetType()))
                .map(Notice::getTargetId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> names = ids.isEmpty() ? Map.of()
                : clazzMapper.selectBatchIds(ids).stream()
                        .collect(Collectors.toMap(Clazz::getId, Clazz::getName, (a, b) -> a));
        list.forEach(n -> n.setTargetLabel(Notice.TARGET_CLASS.equals(n.getTargetType())
                ? names.getOrDefault(n.getTargetId(), "指定班级")
                : "全园"));
    }

    /**
     * 状态、发布时间、可见范围的统一校验。
     *
     * <p>核心规则：状态决定时间含义，两者不能互相矛盾——
     * 「定时待发」的时间必须在未来，「已发布」的时间必须在现在或过去。
     */
    private void normalize(Notice entity) {
        LocalDateTime now = nowSeconds();
        Integer status = entity.getStatus();
        if (status == null) {
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

        // 可见范围
        String targetType = entity.getTargetType();
        if (!StringUtils.hasText(targetType)) {
            targetType = Notice.TARGET_ALL;
        }
        if (!Notice.TARGET_ALL.equals(targetType) && !Notice.TARGET_CLASS.equals(targetType)) {
            throw new BizException("可见范围不合法");
        }
        if (Notice.TARGET_CLASS.equals(targetType)) {
            if (entity.getTargetId() == null) {
                throw new BizException("请选择公告可见的班级");
            }
            if (clazzMapper.selectById(entity.getTargetId()) == null) {
                throw new BizException("所选班级不存在，请重新选择");
            }
        } else {
            entity.setTargetId(null);
        }
        entity.setTargetType(targetType);
    }

    /**
     * 截断到秒的当前时间。
     *
     * <p>为什么必须截断：{@code notice.publish_time} 是 {@code DATETIME}（无小数秒），
     * MySQL 写入时会把 {@code .5} 以上四舍五入到下一秒（如 16:11:46.7 → 16:11:47）。
     * 若直接用带纳秒的 now() 写库，紧接着的「已发布且 publish_time <= now()」查询
     * 就可能把刚发布的公告判为"还没到时间"，出现最长 1 秒的不可见窗口。
     */
    private static LocalDateTime nowSeconds() {
        return LocalDateTime.now().withNano(0);
    }
}
