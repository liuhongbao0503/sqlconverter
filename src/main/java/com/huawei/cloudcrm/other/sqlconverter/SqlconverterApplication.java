package com.huawei.cloudcrm.other.sqlconverter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SqlconverterApplication {

    public static void main(String[] args) {
        SpringApplication.run(SqlconverterApplication.class, args);
    }

}
