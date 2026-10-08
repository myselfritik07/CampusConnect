import { defineConfig } from 'vite';

export default defineConfig({
  base: './',
  server: {
    proxy: {
      // Forward API calls to the Java server during local development.
      '/api': 'http://localhost:8080',
    },
  },
});
