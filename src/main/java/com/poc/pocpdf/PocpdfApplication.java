package com.poc.pocpdf;

import com.poc.pocpdf.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(AppProperties.class)
@SpringBootApplication
public class PocpdfApplication {
    public static void main(String[] args) {
        SpringApplication.run(PocpdfApplication.class, args);
    }
}
