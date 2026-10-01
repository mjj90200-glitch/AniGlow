-- AniList 数据源支持：为 anime 表新增 AniList ID（与 mal_id 并存，作为跨源去重键）
ALTER TABLE anime ADD COLUMN anilist_id BIGINT NULL;
CREATE UNIQUE INDEX uk_anime_anilist_id ON anime (anilist_id);
