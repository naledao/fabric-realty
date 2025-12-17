package com.togettoyou.fabricrealty.springbootserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SpringbootServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringbootServerApplication.class, args);
    }
}

