package com.sharath.banking.common.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan("com.sharath.banking.common")
@EnableJpaRepositories("com.sharath.banking.common")
public class CommonConfig {
}
