ALTER TABLE anime
    ADD FULLTEXT INDEX idx_ft_anime_titles (title, title_english, title_cn, search_aliases);
