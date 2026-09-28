package com.studyroom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("com.studyroom.mapper")
public class WhatToEatApplication {
    public static void main(String[] args) {
        SpringApplication.run(WhatToEatApplication.class, args);
    }
}
