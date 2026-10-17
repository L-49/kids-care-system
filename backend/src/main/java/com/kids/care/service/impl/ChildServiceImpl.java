package com.kids.care.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.kids.care.common.BizException;
import com.kids.care.common.ChildCompleteness;
import com.kids.care.dto.ImportResult;
import com.kids.care.entity.Child;
import com.kids.care.mapper.ChildMapper;
import com.kids.care.service.IChildService;
import com.kids.care.entity.Clazz;
import com.kids.care.mapper.ClazzMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    @Override
    public IPage<Child> pageQuery(int pageNum, int pageSize, String keyword, Long clazzId,
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
        IPage<Child> page = page(new Page<>(pageNum, pageSize), qw);
        fillClazzName(page.getRecords());
        fillCompleteness(page.getRecords());
        return page;
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
    public void saveChild(Child entity) {
        normalize(entity);
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        save(entity);
    }

    @Override
    public void updateChild(Child entity) {
        if (entity.getId() == null) {
            throw new BizException("缺少幼儿编号，无法修改");
        }
        if (getById(entity.getId()) == null) {
            throw new BizException("该幼儿档案不存在或已删除");
        }
        normalize(entity);
        updateById(entity);
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
}
