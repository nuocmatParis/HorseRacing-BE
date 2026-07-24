package com.swp391.horseracing.config;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RaceNameConstraintMigration implements CommandLineRunner {

    JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            List<Map<String, Object>> indexes = jdbcTemplate.queryForList(
                    "SELECT DISTINCT INDEX_NAME FROM INFORMATION_SCHEMA.STATISTICS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'races' AND COLUMN_NAME = 'name' AND NON_UNIQUE = 0"
            );

            for (Map<String, Object> index : indexes) {
                String indexName = (String) index.get("INDEX_NAME");
                if (indexName != null && !indexName.equalsIgnoreCase("PRIMARY")) {
                    log.info("Dropping legacy unique index {} on races.name", indexName);
                    jdbcTemplate.execute("ALTER TABLE races DROP INDEX `" + indexName + "`");
                }
            }
        } catch (Exception e) {
            log.warn("Could not automatically drop unique index on races.name: {}", e.getMessage());
        }
    }
}
