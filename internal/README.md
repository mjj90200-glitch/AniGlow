# Nuxt internal compatibility shim

`nuxt.config.ts` maps `#internal/nuxt/paths` to `internal/nuxt/paths.mjs` so Vitest and
standalone tooling can resolve the same asset helpers that Nuxt injects during a build.

Keep this file in Git. If Nuxt changes its internal path contract or the application changes
its base URL / build-assets directory, update the shim and its tests in the same pull request.
