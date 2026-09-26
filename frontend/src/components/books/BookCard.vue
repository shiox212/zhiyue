<script setup lang="ts">
import type { Book } from '@/types/domain'

defineProps<{ book: Book; reason?: string }>()

const hideBrokenImage = (event: Event) => {
  const image = event.currentTarget as HTMLImageElement
  image.hidden = true
}
</script>

<template>
  <article class="book-card card">
    <RouterLink class="book-card__link" :to="`/books/${book.id}`" :aria-label="`查看《${book.title}》详情`">
      <div class="book-card__cover">
        <img v-if="book.coverUrl" :src="book.coverUrl" :alt="`《${book.title}》封面`" loading="lazy" @error="hideBrokenImage" />
        <span class="book-card__placeholder" aria-hidden="true">知阅</span>
      </div>
      <div class="book-card__body">
        <div class="cluster book-card__meta">
          <span class="tag">{{ book.category || '未分类' }}</span>
          <span v-if="book.avgScore" class="book-card__score" :aria-label="`平均评分 ${book.avgScore.toFixed(1)} 分`">{{ book.avgScore.toFixed(1) }}</span>
        </div>
        <h2>{{ book.title }}</h2>
        <p class="book-card__author">{{ book.author }}</p>
        <p v-if="reason || book.reason" class="book-card__reason">{{ reason || book.reason }}</p>
      </div>
    </RouterLink>
  </article>
</template>

<style scoped>
.book-card { overflow: hidden; min-width: 0; transition: border-color var(--motion-fast), box-shadow var(--motion-fast); }
.book-card:hover { border-color: var(--color-border-strong); box-shadow: var(--shadow-md); }
.book-card__link { display: block; height: 100%; text-decoration: none; }
.book-card__cover { position: relative; aspect-ratio: 4 / 5; overflow: hidden; background: linear-gradient(145deg, var(--color-primary-soft), var(--color-canvas-soft)); }
.book-card__cover img { position: relative; z-index: 1; width: 100%; height: 100%; object-fit: cover; }
.book-card__placeholder { position: absolute; inset: 0; display: grid; place-items: center; color: var(--color-primary); font-family: var(--font-reading); font-size: 1.35rem; font-weight: 800; letter-spacing: .22em; }
.book-card__body { display: grid; gap: 6px; padding: 16px; }
.book-card__meta { justify-content: space-between; }
.book-card__score { color: var(--color-accent); font-weight: 800; font-variant-numeric: tabular-nums; }
.book-card h2 { display: -webkit-box; overflow: hidden; margin: 4px 0 0; font-family: var(--font-reading); font-size: 1.1rem; line-height: 1.45; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.book-card__author { margin: 0; color: var(--color-text-soft); font-size: .9rem; }
.book-card__reason { margin: 6px 0 0; border-top: 1px solid var(--color-border); padding-top: 10px; color: var(--color-text-soft); font-size: .84rem; }
</style>
