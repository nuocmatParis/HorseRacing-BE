SET FOREIGN_KEY_CHECKS = 0;

SET SESSION group_concat_max_len = 1000000;

SELECT GROUP_CONCAT(
               CONCAT('`', table_name, '`')
               SEPARATOR ', '
       )
INTO @table_names
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_type = 'BASE TABLE';

SET @drop_sql = IF(
        @table_names IS NULL,
        'SELECT "Database không có table nào";',
        CONCAT('DROP TABLE IF EXISTS ', @table_names, ';')
                );

PREPARE drop_statement FROM @drop_sql;
EXECUTE drop_statement;
DEALLOCATE PREPARE drop_statement;

SET FOREIGN_KEY_CHECKS = 1;