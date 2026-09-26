<script setup lang="ts">
import { RouterView } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
</script>

<template>
  <a href="#main-content" class="skip-link">跳到主要内容</a>
  <AppHeader />
  <main id="main-content" tabindex="-1">
    <RouterView />
  </main>
  <footer class="site-footer">
    <div class="container footer-inner">
      <p><strong>知阅</strong> · 让阅读、记录与发现自然发生。</p>
      <p>课程实践升级版</p>
    </div>
  </footer>
</template>

<style scoped>
.site-footer { border-top: 1px solid var(--color-border); background: var(--color-canvas-soft); color: var(--color-text-soft); }
.footer-inner { display: flex; min-height: 78px; flex-direction: column; justify-content: center; gap: 2px; padding-block: 16px; }
.footer-inner p { margin: 0; }
.footer-inner strong { color: var(--color-text); font-family: var(--font-reading); letter-spacing: .08em; }
@media (min-width: 640px) { .footer-inner { flex-direction: row; align-items: center; justify-content: space-between; } }
</style>
