package com.kids.care.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.kids.care.common.BizException;
import com.kids.care.common.ChildCompleteness;
import com.kids.care.common.ExcelExportSupport;
import com.kids.care.common.UserContext;
import com.kids.care.dto.ImportResult;
import com.kids.care.entity.Attendance;
import com.kids.care.entity.Child;
import com.kids.care.entity.ChildChangeLog;
import com.kids.care.entity.Fee;
import com.kids.care.entity.LeaveRequest;
import com.kids.care.mapper.AttendanceMapper;
import com.kids.care.mapper.ChildChangeLogMapper;
import com.kids.care.mapper.ChildMapper;
import com.kids.care.mapper.FeeMapper;
import com.kids.care.mapper.LeaveRequestMapper;
import com.kids.care.mapper.StatMapper;
import com.kids.care.service.IChildService;
import com.kids.care.entity.Clazz;
import com.kids.care.mapper.ClazzMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 幼儿档案 业务实现
 */
@Service
@RequiredArgsConstructor
public class ChildServiceImpl extends ServiceImpl<ChildMapper, Child> implements IChildService {

    private final ClazzMapper clazzMapper;
    private final ChildChangeLogMapper changeLogMapper;
    private final AttendanceMapper attendanceMapper;
    private final FeeMapper feeMapper;
    private final LeaveRequestMapper leaveRequestMapper;
    private final StatMapper statMapper;

    @Override
    public Map<String, Object> myChildDashboard(Long childId) {
        Child child = getById(childId);
        if (child == null) {
            throw new BizException("未找到绑定的幼儿档案，请联系幼儿园核实");
        }
        fillClazzName(List.of(child));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("child", child);

        Attendance today = attendanceMapper.selectOne(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getChildId, childId)
                .eq(Attendance::getAttDate, LocalDate.now())
                .last("limit 1"));
        data.put("todayAttendance", today);

        data.put("attendanceDays", statMapper.attendanceDaysOfChild(childId));

        List<Fee> fees = feeMapper.selectList(new LambdaQueryWrapper<Fee>()
                .eq(Fee::getChildId, childId)
                .orderByDesc(Fee::getFeeMonth)
                .last("limit 12"));
        BigDecimal unpaid = fees.stream()
                .filter(f -> f.getPayStatus() != null && f.getPayStatus() == 0)
                .map(Fee::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        data.put("fees", fees);
        data.put("unpaidAmount", unpaid);

        data.put("leaves", leaveRequestMapper.selectList(new LambdaQueryWrapper<LeaveRequest>()
                .eq(LeaveRequest::getChildId, childId)
                .orderByDesc(LeaveRequest::getId)
                .last("limit 10")));
        return data;
    }

    @Override
    public IPage<Child> pageQuery(int pageNum, int pageSize, String keyword, Long clazzId,
                                  Integer status, Boolean incompleteOnly) {
        IPage<Child> page = page(new Page<>(pageNum, pageSize),
                buildWrapper(keyword, clazzId, status, incompleteOnly));
        fillClazzName(page.getRecords());
        fillCompleteness(page.getRecords());
        return page;
    }

    /**
     * 列表与导出共用同一套筛选条件与排序。
     *
     * <p>抽出来是有意的：如果导出另写一遍条件，日后改了一处忘了另一处，
     * 就会出现「列表看到的」和「导出的」不是同一批数据 —— 这种问题很难被发现。
     */
    private LambdaQueryWrapper<Child> buildWrapper(String keyword, Long clazzId,
                                                   Integer status, Boolean incompleteOnly) {
        LambdaQueryWrapper<Child> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Child::getName, keyword)
                    .or().like(Child::getGuardianName, keyword)
                    .or().like(Child::getGuardianPhone, keyword));
        }
        if (clazzId != null) {
            qw.eq(Child::getClazzId, clazzId);
        }
        if (status != null) {
            qw.eq(Child::getStatus, status);
        }
        if (Boolean.TRUE.equals(incompleteOnly)) {
            // 用 SQL 侧算完整度，保证分页 total 与筛选结果一致（在 Java 里过滤会导致条数错乱）
            qw.apply(ChildCompleteness.incompleteSql());
        }
        qw.orderByAsc(Child::getClazzId).orderByAsc(Child::getId);
        return qw;
    }

    @Override
    public List<Child> listAll() {
        List<Child> list = list(new LambdaQueryWrapper<Child>()
                .eq(Child::getStatus, 1).orderByAsc(Child::getClazzId).orderByAsc(Child::getId));
        fillClazzName(list);
        return list;
    }

    @Override
    public List<Child> listAllergy() {
        List<Child> list = list(new LambdaQueryWrapper<Child>()
                .eq(Child::getStatus, 1)
                .isNotNull(Child::getAllergy)
                .ne(Child::getAllergy, "")
                .orderByAsc(Child::getClazzId));
        fillClazzName(list);
        return list;
    }

    private void fillClazzName(List<Child> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> ids = list.stream().map(Child::getClazzId).filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, String> names = clazzMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Clazz::getId, Clazz::getName));
        list.forEach(c -> c.setClazzName(names.get(c.getClazzId())));
    }

    /** 填充档案完整度（实时计算，不落库） */
    private void fillCompleteness(List<Child> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (Child c : list) {
            c.setCompleteness(ChildCompleteness.score(c));
            c.setMissingFields(ChildCompleteness.missingLabels(c));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveChild(Child entity) {
        normalize(entity);
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        save(entity);
        changeLogMapper.insert(wholeLog(entity, ChildChangeLog.TYPE_CREATE, "创建档案", null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateChild(Child entity) {
        if (entity.getId() == null) {
            throw new BizException("缺少幼儿编号，无法修改");
        }
        Child old = getById(entity.getId());
        if (old == null) {
            throw new BizException("该幼儿档案不存在或已删除");
        }
        normalize(entity);
        updateById(entity);
        // 字段级差异留痕：先拿旧值再对比，updateById 是直接覆盖，事后拿不到旧值
        List<ChildChangeLog> logs = diff(old, entity, UserContext.displayName());
        logs.forEach(changeLogMapper::insert);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transfer(Long childId, Long targetClazzId) {
        Child child = getById(childId);
        if (child == null) {
            throw new BizException("幼儿不存在或已删除");
        }
        if (child.getStatus() == null || child.getStatus() != 1) {
            throw new BizException("幼儿【" + child.getName() + "】已离园，不能调班");
        }
        if (targetClazzId == null) {
            throw new BizException("请选择要调入的班级");
        }
        if (targetClazzId.equals(child.getClazzId())) {
            throw new BizException("幼儿已在该班级中，无需调班");
        }
        Clazz target = clazzMapper.selectById(targetClazzId);
        if (target == null) {
            throw new BizException("目标班级不存在或已删除");
        }
        long current = count(new LambdaQueryWrapper<Child>()
                .eq(Child::getClazzId, targetClazzId)
                .eq(Child::getStatus, 1));
        if (target.getCapacity() != null && current >= target.getCapacity()) {
            throw new BizException("班级【" + target.getName() + "】已满 " + target.getCapacity()
                    + " 人，无法调入，请先调整容量或选择其他班级");
        }
        Long fromClazzId = child.getClazzId();
        Child update = new Child();
        update.setId(childId);
        update.setClazzId(targetClazzId);
        updateById(update);
        // 调班留痕：班级是档案的关键字段，家长与教师都关心"谁把孩子调到了哪个班"
        ChildChangeLog log = new ChildChangeLog();
        log.setChildId(childId);
        log.setChildName(child.getName());
        log.setChangeType(ChildChangeLog.TYPE_UPDATE);
        log.setFieldName("clazzId");
        log.setFieldLabel("所在班级（调班）");
        log.setOldValue(clazzName(fromClazzId));
        log.setNewValue(target.getName());
        log.setOperator(UserContext.displayName());
        changeLogMapper.insert(log);
    }

    /** 统一整理入参：去空格、补默认性别、校验班级存在 */
    private void normalize(Child entity) {
        if (StringUtils.hasText(entity.getName())) {
            entity.setName(entity.getName().trim());
        }
        if (!StringUtils.hasText(entity.getGender())) {
            entity.setGender("男");
        }
        if (entity.getClazzId() != null && clazzMapper.selectById(entity.getClazzId()) == null) {
            throw new BizException("所选班级不存在，请重新选择");
        }
    }

    // ==================== Excel 批量导入 ====================

    /** 导入模板表头，顺序即列顺序；校验时逐列比对，防止用错模板导致整列错位 */
    private static final List<String> IMPORT_HEADERS = List.of(
            "幼儿姓名", "性别", "出生日期", "所在班级",
            "家长姓名", "家长电话", "家庭住址", "过敏史", "入园日期");

    private static final DateTimeFormatter FMT_DASH = DateTimeFormatter.ofPattern("yyyy-M-d");
    private static final DateTimeFormatter FMT_SLASH = DateTimeFormatter.ofPattern("yyyy/M/d");
    private static final Pattern FMT_CN =
            Pattern.compile("^(\\d{4})年(\\d{1,2})月(\\d{1,2})日$");
    private static final Pattern FMT_EXCEL_SERIAL = Pattern.compile("^\\d{5}(\\.0+)?$");
    private static final String PHONE_REGEX = "^1[3-9]\\d{9}$";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResult importChildren(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要导入的 Excel 文件");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!filename.endsWith(".xlsx") && !filename.endsWith(".xls")) {
            throw new BizException("只支持 .xlsx 或 .xls 格式的 Excel 文件");
        }

        List<Map<Integer, String>> rows;
        try (InputStream in = file.getInputStream()) {
            // headRowNumber(0)：把表头也当作数据读出来，先校验表头再逐行解析
            rows = EasyExcel.read(in).headRowNumber(0).sheet().doReadSync();
        } catch (Exception e) {
            throw new BizException("Excel 解析失败，请确认文件未损坏：" + e.getMessage());
        }
        if (rows == null || rows.isEmpty()) {
            throw new BizException("Excel 内容为空");
        }
        checkHeader(rows.get(0));

        // 班级名 → 班级 ID；档案去重键（姓名 + 家长电话）
        Map<String, Long> clazzMap = clazzMapper.selectList(null).stream()
                .collect(Collectors.toMap(Clazz::getName, Clazz::getId, (a, b) -> a));
        Set<String> exists = list(new LambdaQueryWrapper<Child>()
                .select(Child::getName, Child::getGuardianPhone))
                .stream().map(c -> dupKey(c.getName(), c.getGuardianPhone()))
                .collect(Collectors.toSet());

        ImportResult result = new ImportResult();
        List<Child> toSave = new ArrayList<>();
        Set<String> batchKeys = new HashSet<>();
        int total = 0;

        for (int i = 1; i < rows.size(); i++) {
            Map<Integer, String> row = rows.get(i);
            if (isBlankRow(row)) {
                continue;   // 完全空行不计入总数
            }
            total++;
            int rowNum = i + 1;   // 表头是第 1 行
            String name = cell(row, 0);
            try {
                Child child = parseRow(row, clazzMap);
                String key = dupKey(child.getName(), child.getGuardianPhone());
                if (exists.contains(key)) {
                    throw new BizException("同名同电话的幼儿已存在，如需修改请用编辑功能");
                }
                if (!batchKeys.add(key)) {
                    throw new BizException("与文件中的其他行重复（同名同电话）");
                }
                toSave.add(child);
            } catch (BizException e) {
                result.addError(rowNum, name, e.getMessage());
            }
        }

        if (!toSave.isEmpty()) {
            saveBatch(toSave);
            // 一次导入共用一个批次号，便于按批次追溯"这批是谁在什么时候导入的"
            String batchId = "IMP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            for (Child c : toSave) {
                changeLogMapper.insert(wholeLog(c, ChildChangeLog.TYPE_IMPORT, "Excel 批量导入", batchId));
            }
            result.setBatchId(batchId);
        }
        result.setTotal(total);
        result.setSuccessCount(toSave.size());
        return result;
    }

    @Override
    public void writeImportTemplate(OutputStream out) {
        List<List<String>> head = IMPORT_HEADERS.stream().map(h -> List.of(h)).collect(Collectors.toList());
        List<List<String>> sample = new ArrayList<>();
        sample.add(List.of("张小明", "男", "2021-05-12", "小班", "张建国",
                "13800000003", "朝阳区阳光小区1-201", "芒果过敏", "2024-09-01"));
        EasyExcel.write(out)
                .head(head)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet("幼儿档案")
                .doWrite(sample);
    }

    // ==================== Excel 导出 ====================

    /** 导出列顺序即表头顺序，与列表展示对齐（列表里的操作列除外） */
    private static final List<String> EXPORT_HEADERS = List.of(
            "幼儿姓名", "性别", "出生日期", "所在班级", "家长姓名", "家长电话",
            "家庭住址", "过敏史", "入园日期", "在园状态", "档案完整度", "待补全项", "建档时间");

    /** 单次导出上限：xlsx 在内存里拼，无上限的话一次导出几万行会把堆撑爆（可用配置覆盖） */
    @Value("${kids.export.max-rows:5000}")
    private int exportMaxRows;

    @Override
    public void exportChildren(String keyword, Long clazzId, Integer status,
                               Boolean incompleteOnly, OutputStream out) {
        LambdaQueryWrapper<Child> qw = buildWrapper(keyword, clazzId, status, incompleteOnly);
        long total = count(qw);
        if (total == 0) {
            throw new BizException("当前筛选条件下没有可导出的数据");
        }
        if (total > exportMaxRows) {
            throw new BizException("当前筛选结果共 " + total + " 条，超过单次导出上限 "
                    + exportMaxRows + " 条，请缩小范围（如先选班级）后再导出");
        }
        List<Child> list = list(qw);
        fillClazzName(list);
        fillCompleteness(list);

        List<List<Object>> rows = new ArrayList<>(list.size());
        for (Child c : list) {
            rows.add(toExportRow(c));
        }
        ExcelExportSupport.write(out, "幼儿档案", EXPORT_HEADERS, rows);
    }

    /** 单行导出数据：字典值翻中文、日期统一 yyyy-MM-dd、空值写空串（不写 null 以免出现 "null"） */
    private List<Object> toExportRow(Child c) {
        String missing = ChildCompleteness.missingLabels(c);
        return Arrays.asList(
                nz(c.getName()),
                nz(c.getGender()),
                dateText(c.getBirthday()),
                nz(c.getClazzName()),
                nz(c.getGuardianName()),
                nz(c.getGuardianPhone()),
                nz(c.getAddress()),
                nz(c.getAllergy()),
                dateText(c.getEnrollDate()),
                statusText(c.getStatus()),
                ChildCompleteness.score(c) + "%",
                missing.isEmpty() ? "已完整" : missing,
                dateTimeText(c.getCreateTime()));
    }

    private static String statusText(Integer status) {
        if (status == null) {
            return "";
        }
        return status == 1 ? "在园" : "离园";
    }

    private static String dateText(LocalDate d) {
        return d == null ? "" : d.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private static String dateTimeText(LocalDateTime t) {
        return t == null ? "" : t.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    /** 逐列校验表头，避免用户用错模板导致数据整列错位却静默导入 */
    private void checkHeader(Map<Integer, String> header) {
        for (int i = 0; i < IMPORT_HEADERS.size(); i++) {
            String actual = cell(header, i);
            if (!IMPORT_HEADERS.get(i).equals(actual)) {
                throw new BizException("模板表头不正确：第 " + (i + 1) + " 列应为「" + IMPORT_HEADERS.get(i)
                        + "」，实际是「" + (actual.isEmpty() ? "空" : actual) + "」。请下载最新模板后重试");
            }
        }
    }

    private Child parseRow(Map<Integer, String> row, Map<String, Long> clazzMap) {
        Child c = new Child();

        String name = cell(row, 0);
        if (name.isEmpty()) {
            throw new BizException("幼儿姓名不能为空");
        }
        if (name.length() > 50) {
            throw new BizException("幼儿姓名不能超过 50 个字符");
        }
        c.setName(name);

        String gender = cell(row, 1);
        if (gender.isEmpty()) {
            gender = "男";
        }
        if (!"男".equals(gender) && !"女".equals(gender)) {
            throw new BizException("性别只能填「男」或「女」");
        }
        c.setGender(gender);

        c.setBirthday(parseDate(cell(row, 2), "出生日期"));
        if (c.getBirthday() != null && c.getBirthday().isAfter(LocalDate.now())) {
            throw new BizException("出生日期不能晚于今天");
        }

        String clazzName = cell(row, 3);
        if (clazzName.isEmpty()) {
            throw new BizException("所在班级不能为空");
        }
        Long clazzId = clazzMap.get(clazzName);
        if (clazzId == null) {
            throw new BizException("班级「" + clazzName + "」不存在，请先在班级管理中添加");
        }
        c.setClazzId(clazzId);

        String guardianName = cell(row, 4);
        if (guardianName.length() > 50) {
            throw new BizException("家长姓名不能超过 50 个字符");
        }
        c.setGuardianName(guardianName.isEmpty() ? null : guardianName);

        String phone = cell(row, 5);
        if (!phone.isEmpty() && !phone.matches(PHONE_REGEX)) {
            throw new BizException("家长电话格式不正确（应为 11 位手机号）");
        }
        c.setGuardianPhone(phone.isEmpty() ? null : phone);

        String address = cell(row, 6);
        if (address.length() > 200) {
            throw new BizException("家庭住址不能超过 200 个字符");
        }
        c.setAddress(address.isEmpty() ? null : address);

        String allergy = cell(row, 7);
        if (allergy.length() > 200) {
            throw new BizException("过敏史不能超过 200 个字符");
        }
        c.setAllergy(allergy.isEmpty() ? null : allergy);

        c.setEnrollDate(parseDate(cell(row, 8), "入园日期"));
        c.setStatus(1);
        return c;
    }

    /**
     * 解析日期：兼容 yyyy-MM-dd、yyyy/M/d、yyyy年M月d日，
     * 以及 Excel 把日期存成序列号时导出的纯数字（如 45870）。
     */
    private LocalDate parseDate(String raw, String label) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            Matcher cn = FMT_CN.matcher(s);
            if (cn.matches()) {
                return LocalDate.of(Integer.parseInt(cn.group(1)),
                        Integer.parseInt(cn.group(2)), Integer.parseInt(cn.group(3)));
            }
            if (s.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
                return LocalDate.parse(s, FMT_DASH);
            }
            if (s.matches("\\d{4}/\\d{1,2}/\\d{1,2}")) {
                return LocalDate.parse(s, FMT_SLASH);
            }
            if (FMT_EXCEL_SERIAL.matcher(s).matches()) {
                // Excel 1900 日期系统：序列号 1 对应 1900-01-01，基准取 1899-12-30
                return LocalDate.of(1899, 12, 30).plusDays((long) Double.parseDouble(s));
            }
        } catch (java.time.DateTimeException | NumberFormatException e) {
            throw new BizException(label + "格式不正确，请填写 yyyy-MM-dd");
        }
        throw new BizException(label + "格式不正确，请填写 yyyy-MM-dd");
    }

    private static String cell(Map<Integer, String> row, int idx) {
        String v = row == null ? null : row.get(idx);
        return v == null ? "" : v.trim();
    }

    private static boolean isBlankRow(Map<Integer, String> row) {
        if (row == null) {
            return true;
        }
        for (String v : row.values()) {
            if (v != null && !v.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static String dupKey(String name, String phone) {
        return (name == null ? "" : name.trim()) + "|" + (phone == null ? "" : phone.trim());
    }

    // ==================== 变更留痕 ====================

    @Override
    public List<ChildChangeLog> listHistory(Long childId) {
        return changeLogMapper.selectList(new LambdaQueryWrapper<ChildChangeLog>()
                .eq(ChildChangeLog::getChildId, childId)
                .orderByDesc(ChildChangeLog::getCreateTime)
                .orderByDesc(ChildChangeLog::getId));
    }

    /**
     * 字段级差异对比。只留痕 8 个关键字段，班级比较的是 id 但展示名称。
     *
     * <p>过敏史单独说明：膳食模块直接读 {@code child.allergy} 来标注过敏幼儿，
     * 家长临时补充过敏信息时，必须能查到"谁在什么时候改的"。
     */
    private List<ChildChangeLog> diff(Child old, Child now, String operator) {
        List<ChildChangeLog> logs = new ArrayList<>();
        addDiff(logs, old, now, operator, "name", "幼儿姓名", old.getName(), now.getName());
        addDiff(logs, old, now, operator, "gender", "性别", old.getGender(), now.getGender());
        addDiff(logs, old, now, operator, "birthday", "出生日期",
                str(old.getBirthday()), str(now.getBirthday()));
        if (!Objects.equals(old.getClazzId(), now.getClazzId())) {
            addDiff(logs, old, now, operator, "clazzId", "所在班级",
                    clazzName(old.getClazzId()), clazzName(now.getClazzId()));
        }
        addDiff(logs, old, now, operator, "guardianName", "家长姓名",
                old.getGuardianName(), now.getGuardianName());
        addDiff(logs, old, now, operator, "guardianPhone", "家长电话",
                old.getGuardianPhone(), now.getGuardianPhone());
        addDiff(logs, old, now, operator, "address", "家庭住址", old.getAddress(), now.getAddress());
        addDiff(logs, old, now, operator, "allergy", "过敏史", old.getAllergy(), now.getAllergy());
        return logs;
    }

    private void addDiff(List<ChildChangeLog> logs, Child old, Child now, String operator,
                         String field, String label, String oldVal, String newVal) {
        if (Objects.equals(oldVal, newVal)) {
            return;
        }
        ChildChangeLog log = new ChildChangeLog();
        log.setChildId(now.getId() != null ? now.getId() : old.getId());
        log.setChildName(nvl(now.getName(), old.getName()));
        log.setChangeType(ChildChangeLog.TYPE_UPDATE);
        log.setFieldName(field);
        log.setFieldLabel(label);
        log.setOldValue(oldVal);
        log.setNewValue(newVal);
        log.setOperator(operator);
        logs.add(log);
    }

    private String clazzName(Long id) {
        if (id == null) {
            return null;
        }
        Clazz c = clazzMapper.selectById(id);
        return c == null ? ("#" + id) : c.getName();
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static String nvl(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }

    /** 整条记录（新增 / 导入）只写一行，不做字段级展开，避免日志表被建库时的 8 行噪音淹没 */
    private ChildChangeLog wholeLog(Child c, String type, String desc, String batchId) {
        ChildChangeLog log = new ChildChangeLog();
        log.setChildId(c.getId());
        log.setChildName(c.getName());
        log.setChangeType(type);
        log.setFieldLabel("—");
        log.setNewValue(desc);
        log.setOperator(UserContext.displayName());
        log.setBatchId(batchId);
        return log;
    }
}
