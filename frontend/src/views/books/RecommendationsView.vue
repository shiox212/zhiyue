<script setup lang="ts">
import { onMounted, ref } from 'vue'
import BookCard from '@/components/books/BookCard.vue'
import StatusPanel from '@/components/base/StatusPanel.vue'
import { recommendApi } from '@/api/recommend.api'
import { booksApi } from '@/api/books.api'
import { useAuthStore } from '@/stores/auth.store'
import type { Book } from '@/types/domain'
import { getErrorMessage } from '@/utils/format'

const auth = useAuthStore()
const books = ref<Book[]>([])
const loading = ref(true)
const error = ref('')
const isPersonalized = ref(true)

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    books.value = await recommendApi.list(auth.user!.id)
    if (!books.value.length) {
      books.value = (await booksApi.list()).slice(0, 8)
      isPersonalized.value = false
    }
  } catch (cause) {
    error.value = getErrorMessage(cause, '推荐内容暂时无法加载')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page-shell container">
    <header class="page-heading">
      <div>
        <p class="eyebrow">FOR YOUR SHELF</p>
        <h1>为你推荐</h1>
        <p class="lede">结合你的评分和书架偏好，找到更接近当下兴趣的读物。</p>
      </div>
    </header>

    <StatusPanel v-if="loading" tone="loading" title="正在整理推荐" description="根据你的阅读记录匹配合适的图书。" />
    <StatusPanel v-else-if="error" tone="error" title="推荐加载失败" :description="error">
      <button class="button secondary" type="button" @click="load">重新加载</button>
    </StatusPanel>
    <template v-else>
      <p v-if="!isPersonalized" class="recommend-note" role="status">你的互动记录还不够多，当前先展示热门馆藏。完成几次评分后，推荐会更贴近你。</p>
      <div class="book-grid">
        <BookCard v-for="book in books" :key="book.id" :book="book" :reason="book.reason || undefined" />
      </div>
    </template>
  </div>
</template>

<style scoped>
.recommend-note { margin: 0 0 24px; border-left: 4px solid var(--color-accent); border-radius: var(--radius-sm); background: var(--color-accent-soft); padding: 14px 16px; color: #6d3b1e; }
</style>
