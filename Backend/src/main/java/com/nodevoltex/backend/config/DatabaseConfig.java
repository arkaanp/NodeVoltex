package com.nodevoltex.backend.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseConfig {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @PostConstruct
    public void init() {
        if (datasourceUrl.contains("/postgres")) {
            jdbcTemplate.execute("CREATE DATABASE nodevoltex_db");
            System.out.println("Database 'nodevoltex_db' created successfully.");
        }
    }
}
