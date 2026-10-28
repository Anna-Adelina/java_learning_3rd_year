package com.booktracker.booktracker.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class QuartzSchemaIT {

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    Environment env;

    @Test
    void quartzTablesAreCreatedByLiquibase() {
        Integer count = jdbc.queryForObject(
                "select count(*) from information_schema.tables "
                        + "where table_schema = 'PUBLIC' and table_name like 'QRTZ\\_%'", Integer.class);
        assertEquals(11, count);
    }

    @Test
    void springBootIsConfiguredNotToCreateQuartzSchema() {
        assertEquals("never", env.getProperty("spring.quartz.jdbc.initialize-schema"));
        assertEquals("jdbc", env.getProperty("spring.quartz.job-store-type"));
    }
}