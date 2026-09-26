<template>
  <div class="page-shell container narrow">
    <section class="card system-state">
      <p class="system-code">404</p>
      <h1>这一页没有被找到</h1>
      <p>链接可能已经失效，或者地址输入有误。你可以回到图书页继续浏览。</p>
      <RouterLink to="/books" class="button">返回图书页</RouterLink>
    </section>
  </div>
</template>

<style scoped>
.narrow { max-width: 760px; }
.system-state { display: grid; justify-items: start; gap: 16px; padding: clamp(28px, 7vw, 64px); }
.system-code { margin: 0; color: var(--color-accent); font-size: 1rem; font-weight: 800; letter-spacing: .16em; }
.system-state h1 { margin: 0; font-family: var(--font-reading); font-size: clamp(1.8rem, 5vw, 2.8rem); }
.system-state > p:not(.system-code) { max-width: 55ch; margin: 0; color: var(--color-text-soft); }
</style>
