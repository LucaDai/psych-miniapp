package com.psych.miniapp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.psych.miniapp.**.mapper")
public class PsychMiniappApplication {

    public static void main(String[] args) {
        SpringApplication.run(PsychMiniappApplication.class, args);
    }
}
