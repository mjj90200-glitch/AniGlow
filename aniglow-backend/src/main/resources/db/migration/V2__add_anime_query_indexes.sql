SET @has_year_season_index = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'anime' AND index_name = 'idx_anime_year_season'
);
SET @year_season_sql = IF(
    @has_year_season_index = 0,
    'CREATE INDEX idx_anime_year_season ON anime (year, season)',
    'SELECT 1'
);
PREPARE year_season_statement FROM @year_season_sql;
EXECUTE year_season_statement;
DEALLOCATE PREPARE year_season_statement;

SET @has_status_index = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'anime' AND index_name = 'idx_anime_status'
);
SET @status_sql = IF(
    @has_status_index = 0,
    'CREATE INDEX idx_anime_status ON anime (status)',
    'SELECT 1'
);
PREPARE status_statement FROM @status_sql;
EXECUTE status_statement;
DEALLOCATE PREPARE status_statement;
