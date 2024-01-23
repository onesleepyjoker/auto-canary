package com.iscas.autoCanary;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动类
 *
 * @author 一只小小丑
 * @from
 */
@SpringBootApplication
@MapperScan("com.iscas.autoCanary.mapper")
public class AutoCanaryApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoCanaryApplication.class, args);
    }

}

