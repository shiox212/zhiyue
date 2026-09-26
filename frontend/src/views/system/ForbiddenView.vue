<template>
  <div class="page-shell container narrow">
    <section class="card system-state" role="alert">
      <p class="system-code">403</p>
      <h1>你没有访问这个页面的权限</h1>
      <p>当前账户可以继续浏览和管理自己的阅读内容，后台功能仅对管理员开放。</p>
      <div class="cluster">
        <RouterLink to="/books" class="button">返回图书页</RouterLink>
        <RouterLink to="/profile" class="button secondary">查看账户</RouterLink>
      </div>
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
