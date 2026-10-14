import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode, command }) => {
  const env = loadEnv(mode, '.', '');
  const target = env.API_PROXY_TARGET;
  if (command === 'build') return { plugins: [react()] };
  if (!target) throw new Error('Set API_PROXY_TARGET in the frontend .env or .env.local file.');
  const proxy = { '/api': { target, changeOrigin: true } };
  return { plugins: [react()], server: { proxy }, preview: { proxy } };
});
