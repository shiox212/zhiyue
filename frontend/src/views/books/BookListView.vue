<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import BookCard from '@/components/books/BookCard.vue'
import StatusPanel from '@/components/base/StatusPanel.vue'
import { useBooksStore } from '@/stores/books.store'
import { getErrorMessage } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const store = useBooksStore()
const { books, categories } = storeToRefs(store)
const keyword = ref(typeof route.query.q === 'string' ? route.query.q : '')
const selectedCategory = ref(typeof route.query.category === 'string' ? route.query.category : '')
const loading = ref(true)
const error = ref('')

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    await Promise.all([
      store.load({ keyword: keyword.value, category: selectedCategory.value }),
      store.loadCategories(),
    ])
  } catch (cause) {
    error.value = getErrorMessage(cause, '图书暂时无法加载')
  } finally {
    loading.value = false
  }
}

const updateQuery = async () => {
  await router.push({
    name: 'books',
    query: {
      ...(keyword.value.trim() ? { q: keyword.value.trim() } : {}),
      ...(selectedCategory.value ? { category: selectedCategory.value } : {}),
    },
  })
}

const clearFilters = async () => {
  keyword.value = ''
  selectedCategory.value = ''
  await router.push({ name: 'books' })
}

watch(() => route.query, () => {
  keyword.value = typeof route.query.q === 'string' ? route.query.q : ''
  selectedCategory.value = typeof route.query.category === 'string' ? route.query.category : ''
  load()
})

onMounted(load)
</script>

<template>
  <div class="page-shell">
    <section class="container books-hero" aria-labelledby="books-title">
      <div>
        <p class="eyebrow">BOOK DISCOVERY</p>
        <h1 id="books-title">找到下一本想读的书</h1>
        <p>按书名、作者或分类查找，先从真正感兴趣的内容开始。</p>
      </div>
      <RouterLink to="/recommendations" class="hero-link">查看个性化推荐 <span aria-hidden="true">→</span></RouterLink>
    </section>

    <div class="container content-grid">
      <aside class="filter-card card" aria-labelledby="filter-title">
        <h2 id="filter-title">筛选图书</h2>
        <form class="stack" role="search" @submit.prevent="updateQuery">
          <div class="form-field">
            <label for="book-keyword">关键词</label>
            <input id="book-keyword" v-model="keyword" class="input" type="search" placeholder="书名或作者" />
          </div>
          <div class="form-field">
            <label for="book-category">分类</label>
            <select id="book-category" v-model="selectedCategory" class="select" @change="updateQuery">
              <option value="">全部分类</option>
              <option v-for="category in categories" :key="category" :value="category">{{ category }}</option>
            </select>
          </div>
          <button class="button" type="submit">查找</button>
          <button v-if="keyword || selectedCategory" class="button secondary" type="button" @click="clearFilters">清除筛选</button>
        </form>
      </aside>

      <section aria-labelledby="result-title">
        <div class="result-heading">
          <div>
            <p class="eyebrow">馆藏</p>
            <h2 id="result-title" class="section-title">{{ keyword || selectedCategory ? '筛选结果' : '全部图书' }}</h2>
          </div>
          <p v-if="!loading && !error" aria-live="polite">共 {{ books.length }} 本</p>
        </div>

        <div v-if="loading" class="book-grid" aria-label="正在加载图书">
          <div v-for="index in 8" :key="index" class="book-skeleton card" aria-hidden="true">
            <div class="skeleton book-skeleton__cover"></div><div class="skeleton book-skeleton__line"></div><div class="skeleton book-skeleton__short"></div>
          </div>
          <span class="visually-hidden" role="status">正在加载图书</span>
        </div>
        <StatusPanel v-else-if="error" tone="error" title="图书加载失败" :description="error">
          <button type="button" class="button secondary" @click="load">重新加载</button>
        </StatusPanel>
        <StatusPanel v-else-if="books.length === 0" tone="empty" title="没有找到匹配图书" description="换一个关键词或清除分类后再试试。">
          <button type="button" class="button secondary" @click="clearFilters">查看全部图书</button>
        </StatusPanel>
        <div v-else class="book-grid">
          <BookCard v-for="book in books" :key="book.id" :book="book" />
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.books-hero { display: flex; flex-direction: column; align-items: start; gap: 20px; margin-bottom: 32px; border-radius: var(--radius-lg); background: var(--color-primary-strong); padding: clamp(24px, 6vw, 52px); color: white; }
.books-hero h1 { max-width: 14ch; margin: 0; font-family: var(--font-reading); font-size: clamp(2rem, 6vw, 3.8rem); line-height: 1.2; }
.books-hero p:not(.eyebrow) { max-width: 56ch; margin: 12px 0 0; color: #dce9e0; }
.books-hero .eyebrow { color: #c5dacb; }
.hero-link { display: inline-flex; min-height: 44px; align-items: center; gap: 12px; border-bottom: 1px solid #bed3c4; color: white; font-weight: 700; text-decoration: none; }
.content-grid { display: grid; gap: 28px; }
.filter-card { height: fit-content; padding: 20px; }
.filter-card h2 { margin: 0 0 18px; font-family: var(--font-reading); font-size: 1.2rem; }
.result-heading { display: flex; align-items: end; justify-content: space-between; gap: 16px; margin-bottom: 20px; }
.result-heading p:last-child { margin: 0; color: var(--color-text-soft); font-size: .9rem; }
.book-skeleton { overflow: hidden; padding-bottom: 18px; }
.book-skeleton__cover { aspect-ratio: 4 / 5; }
.book-skeleton__line { width: calc(100% - 32px); height: 20px; margin: 16px; }
.book-skeleton__short { width: 45%; height: 14px; margin-inline: 16px; }
@media (min-width: 760px) { .books-hero { flex-direction: row; align-items: end; justify-content: space-between; } }
@media (min-width: 940px) { .content-grid { grid-template-columns: 230px minmax(0, 1fr); align-items: start; } .filter-card { position: sticky; top: 96px; } }
</style>
