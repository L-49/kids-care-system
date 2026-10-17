package com.kids.care;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 幼儿园 / 托育机构管理系统 启动类
 */
@EnableScheduling
@SpringBootApplication
@MapperScan("com.kids.care.mapper")
public class KidsCareApplication {
    public static void main(String[] args) {
        SpringApplication.run(KidsCareApplication.class, args);
        System.out.println("幼儿园托育管理系统启动成功：http://localhost:9090");
    }
}
