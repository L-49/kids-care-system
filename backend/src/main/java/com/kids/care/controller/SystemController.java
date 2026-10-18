package com.kids.care.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kids.care.common.BizException;
import com.kids.care.common.RequireRole;
import com.kids.care.common.Result;
import com.kids.care.common.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 系统设置：运行信息查询与数据备份
 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@RequireRole({Roles.ADMIN})
public class SystemController {

    private static final Path BACKUP_DIR = Paths.get("backups");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /** 系统运行信息 */
    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("dbVersion", jdbcTemplate.queryForObject("select version()", String.class));
        List<Map<String, Object>> tables = jdbcTemplate.queryForList(
                "select table_name as tableName, table_rows as tableRows "
                        + "from information_schema.tables where table_schema = database() order by table_name");
        data.put("tables", tables);
        data.put("tableCount", tables.size());
        Runtime rt = Runtime.getRuntime();
        data.put("jvmUsedMb", (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024);
        data.put("jvmMaxMb", rt.maxMemory() / 1024 / 1024);
        data.put("javaVersion", System.getProperty("java.version"));
        data.put("osName", System.getProperty("os.name"));
        data.put("backupCount", countBackups());
        return Result.ok(data);
    }

    /** 执行数据备份：把全部业务表导出为 JSON 文件 */
    @PostMapping("/backup")
    public Result<Map<String, Object>> backup() throws IOException {
        List<String> tables = jdbcTemplate.queryForList(
                "select table_name from information_schema.tables "
                        + "where table_schema = database() order by table_name", String.class);
        Map<String, Object> dump = new LinkedHashMap<>();
        for (String table : tables) {
            dump.put(table, jdbcTemplate.queryForList("select * from " + table));
        }
        Files.createDirectories(BACKUP_DIR);
        String fileName = "kids_care_"
                + DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now()) + ".json";
        Path file = BACKUP_DIR.resolve(fileName);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), dump);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("fileName", fileName);
        data.put("tableCount", tables.size());
        data.put("sizeKb", Files.size(file) / 1024);
        return Result.ok(data);
    }

    /** 备份文件列表 */
    @GetMapping("/backup/list")
    public Result<List<Map<String, Object>>> backupList() throws IOException {
        if (!Files.exists(BACKUP_DIR)) {
            return Result.ok(new ArrayList<>());
        }
        try (Stream<Path> stream = Files.list(BACKUP_DIR)) {
            List<Map<String, Object>> list = stream.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
                    .map(p -> {
                        try {
                            Map<String, Object> item = new LinkedHashMap<>();
                            item.put("fileName", p.getFileName().toString());
                            item.put("sizeKb", Files.size(p) / 1024);
                            item.put("createTime", Files.getLastModifiedTime(p).toString()
                                    .replace("T", " ").substring(0, 19));
                            return item;
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    })
                    .toList();
            return Result.ok(list);
        }
    }

    /** 删除备份文件 */
    @DeleteMapping("/backup/{name}")
    public Result<Void> removeBackup(@PathVariable String name) throws IOException {
        if (name.contains("..") || name.contains("/") || name.contains("\\")) {
            throw new BizException("文件名不合法");
        }
        Files.deleteIfExists(BACKUP_DIR.resolve(name));
        return Result.ok();
    }

    private long countBackups() {
        if (!Files.exists(BACKUP_DIR)) {
            return 0;
        }
        try (Stream<Path> stream = Files.list(BACKUP_DIR)) {
            return stream.filter(Files::isRegularFile).count();
        } catch (IOException e) {
            return 0;
        }
    }
}
