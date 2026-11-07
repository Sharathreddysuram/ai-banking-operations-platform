package com.sharath.banking.decision;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.sharath.banking")
@EntityScan("com.sharath.banking")
@EnableJpaRepositories("com.sharath.banking")
public class DecisionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DecisionServiceApplication.class, args);
    }
}
