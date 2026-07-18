# Phase 2/3 implementation record

## Phase 2

- N+1: anime genres are lazy and batch-fetched; rating/community queries use entity graphs.
- Pagination: anime and rating list endpoints use bounded database pagination with a maximum page size of 100.
- Cache: Redis-backed list, detail, community, and ranking caches use explicit TTLs and fail-open error handling; mutations evict affected read models.
- Indexes: Flyway manages year/season, status, and title full-text indexes. Search uses full-text first and LIKE as a Chinese partial-title fallback.
- Production profile: OSIV and SQL debug output are disabled; Hibernate only validates the Flyway-managed schema.

## Phase 3

- Backend boundaries: anime, auth, community, rating, and rating-reply controllers delegate to transactional services and DTO mappers.
- Frontend boundaries: AIGC desktop/mobile layouts, emotion stickers, anime detail regions, home grids, and community boxes are isolated components. Backend calls share `useApi`.
- Dependencies: Spring Boot is on the maintained 3.5 line, frontend packages are pinned, pnpm is the only lockfile, and the Nuxt internal compatibility shim is documented.
- Database lifecycle: V1 creates the baseline schema; V2/V3 add query indexes. Fresh-schema migration and Hibernate validation are part of release verification.
- Observability: Actuator health/probes, Prometheus, request trace IDs, rolling logs, and container health checks are enabled.
- Tests: backend tests cover service pagination/search, JWT filters, vote settlement, trace IDs, rate limiting, agent quota/SSE, and integration APIs. Frontend tests cover API envelopes, anime localization/API fallback, and persisted auth sessions.

## Release checks

1. Run `pnpm install --frozen-lockfile`, `pnpm test`, `pnpm exec nuxi typecheck`, and `pnpm build`.
2. Run `mvn verify` in `aniglow-backend`.
3. Start against a fresh temporary MySQL schema and confirm all Flyway migrations, `/api/actuator/health`, `/api/anime`, and `/api/communities`.
4. Back up the production database before applying a new migration.
5. Smoke-test the local reverse proxy and public domain after deployment.
