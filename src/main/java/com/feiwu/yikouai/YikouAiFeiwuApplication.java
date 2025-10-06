package com.feiwu.yikouai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.feiwu.yikouai.mapper")
public class YikouAiFeiwuApplication {

    public static void main(String[] args) {
        SpringApplication.run(YikouAiFeiwuApplication.class, args);
    }

}
