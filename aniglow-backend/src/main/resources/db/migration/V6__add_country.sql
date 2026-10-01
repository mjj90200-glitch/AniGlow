-- 番剧筛选：新增国家/地区列
-- 仅给 MAL 独有记录默认「日本」（Jikan/MAL 以日本番剧为主）；
-- AniList 记录的 country 由同步任务按官方 countryOfOrigin 权威写入，不在此默认，保证筛选正确性。
ALTER TABLE anime ADD COLUMN country VARCHAR(50) NULL;
CREATE INDEX idx_anime_country ON anime (country);
UPDATE anime SET country = '日本' WHERE country IS NULL AND anilist_id IS NULL;
