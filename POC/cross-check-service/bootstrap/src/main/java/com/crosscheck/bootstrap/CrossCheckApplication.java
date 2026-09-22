package com.crosscheck.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class CrossCheckApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrossCheckApplication.class, args);
        log.info("Cross Check Service iniciado");
    }
}
