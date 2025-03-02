package com.ideal.oracleexporter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // 启用定时任务，用于定期收集指标
public class OracleExporterApplication {

    public static void main(String[] args) {
        SpringApplication.run(OracleExporterApplication.class, args);
    }

}
