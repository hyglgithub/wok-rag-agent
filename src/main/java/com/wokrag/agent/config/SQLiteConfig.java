package com.wokrag.agent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
@Configuration
public class SQLiteConfig {

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) throws SQLException {
        // Enable foreign keys for SQLite
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            log.info("SQLite foreign keys enabled");
        }
        return new JdbcTemplate(dataSource);
    }
}
