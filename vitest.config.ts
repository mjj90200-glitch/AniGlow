import { defineConfig } from 'vitest/config'

export default defineConfig({
  test: {
    environment: 'happy-dom',
    globals: true,
    include: ['tests/**/*.test.ts'],
    coverage: {
      provider: 'v8',
      include: [
        'composables/useAnimeI18n.ts',
        'utils/api-client.ts',
        'utils/anime-filters.ts',
        'utils/anime-reviews.ts',
      ],
      reporter: ['text', 'json-summary'],
      thresholds: {
        statements: 90,
        branches: 80,
        functions: 90,
        lines: 90,
      },
    },
  },
  resolve: {
    alias: {
      '~': new URL('.', import.meta.url).pathname,
    },
  },
})
