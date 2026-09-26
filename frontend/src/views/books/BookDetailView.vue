<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BookCard from '@/components/books/BookCard.vue'
import StatusPanel from '@/components/base/StatusPanel.vue'
import { booksApi } from '@/api/books.api'
import { shelfApi } from '@/api/shelf.api'
import { engagementApi } from '@/api/engagement.api'
import { useAuthStore } from '@/stores/auth.store'
import { useUiStore } from '@/stores/ui.store'
import type { Book, Review } from '@/types/domain'
import { formatDate, getErrorMessage } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const ui = useUiStore()
const book = ref<Book | null>(null)
const reviews = ref<Review[]>([])
const similar = ref<Book[]>([])
const rating = ref(0)
const reviewContent = ref('')
const loading = ref(true)
const actionPending = ref('')
const error = ref('')
const reviewError = ref('')

const bookId = computed(() => Number(route.params.id))

const load = async () => {
  loading.value = true
  error.value = ''
  book.value = null
  if (!Number.isInteger(bookId.value) || bookId.value <= 0) {
    await router.replace('/404')
    return
  }
  try {
    book.value = await booksApi.getById(bookId.value)
    const [reviewResult, similarResult] = await Promise.allSettled([
      booksApi.reviews(bookId.value),
      booksApi.similar(bookId.value),
    ])
    reviews.value = reviewResult.status === 'fulfilled' ? reviewResult.value : []
    similar.value = similarResult.status === 'fulfilled' ? similarResult.value.filter((item) => item.id !== bookId.value).slice(0, 4) : []
    if (auth.user) rating.value = await engagementApi.getRating(auth.user.id, bookId.value).catch(() => 0)
  } catch (cause) {
    error.value = getErrorMessage(cause, '图书详情暂时无法加载')
  } finally {
    loading.value = false
  }
}

const addToShelf = async () => {
  if (!auth.user) return
  actionPending.value = 'shelf'
  try {
    await shelfApi.save(auth.user.id, bookId.value, 0)
    ui.notify('已加入书架，状态为“想读”', 'success')
  } catch (cause) {
    ui.notify(getErrorMessage(cause), 'error')
  } finally {
    actionPending.value = ''
  }
}

const submitRating = async () => {
  if (!auth.user || rating.value < 1) return
  actionPending.value = 'rating'
  try {
    await engagementApi.rate(auth.user.id, bookId.value, rating.value)
    await shelfApi.save(auth.user.id, bookId.value, 2)
    ui.notify('评分已保存，并将图书标记为“已读”', 'success')
  } catch (cause) {
    ui.notify(getErrorMessage(cause), 'error')
  } finally {
    actionPending.value = ''
  }
}

const submitReview = async () => {
  reviewError.value = ''
  const content = reviewContent.value.trim()
  if (!content) {
    reviewError.value = '请先写下书评内容'
    return
  }
  if (!auth.user) return
  actionPending.value = 'review'
  try {
    await engagementApi.review(auth.user.id, bookId.value, content)
    reviewContent.value = ''
    reviews.value = await booksApi.reviews(bookId.value)
    ui.notify('书评已发布', 'success')
  } catch (cause) {
    reviewError.value = getErrorMessage(cause)
  } finally {
    actionPending.value = ''
  }
}

watch(() => route.params.id, load)
onMounted(load)
</script>

<template>
  <div class="page-shell container">
    <StatusPanel v-if="loading" tone="loading" title="正在打开这本书" />
    <StatusPanel v-else-if="error" tone="error" title="无法显示图书详情" :description="error">
      <div class="cluster"><button class="button secondary" type="button" @click="load">重试</button><RouterLink class="button" to="/books">返回图书页</RouterLink></div>
    </StatusPanel>

    <template v-else-if="book">
      <nav class="breadcrumb" aria-label="面包屑导航"><RouterLink to="/books">发现图书</RouterLink><span aria-hidden="true">/</span><span aria-current="page">{{ book.title }}</span></nav>
      <section class="detail-card card">
        <div class="detail-cover">
          <img v-if="book.coverUrl" :src="book.coverUrl" :alt="`《${book.title}》封面`" />
          <span v-else aria-hidden="true">知阅</span>
        </div>
        <div class="detail-copy">
          <span class="tag">{{ book.category }}</span>
          <h1>{{ book.title }}</h1>
          <p class="detail-author">{{ book.author }} 著</p>
          <div class="book-facts" aria-label="图书互动数据">
            <span><strong>{{ book.avgScore?.toFixed(1) || '暂无' }}</strong>平均评分</span>
            <span><strong>{{ book.ratingCount || 0 }}</strong>次评分</span>
            <span><strong>{{ book.reviewCount || reviews.length }}</strong>篇书评</span>
          </div>
          <p class="description">{{ book.description || '暂无图书简介。' }}</p>
          <div class="cluster">
            <button v-if="auth.isAuthenticated" class="button" type="button" :disabled="actionPending === 'shelf'" @click="addToShelf">{{ actionPending === 'shelf' ? '正在加入…' : '加入书架' }}</button>
            <RouterLink v-else class="button" :to="{ name: 'login', query: { redirect: route.fullPath } }">登录后收藏</RouterLink>
          </div>
        </div>
      </section>

      <div class="detail-columns">
        <section class="card panel" aria-labelledby="reviews-title">
          <div class="section-head"><div><p class="eyebrow">READER NOTES</p><h2 id="reviews-title" class="section-title">读者书评</h2></div><span>{{ reviews.length }} 篇</span></div>

          <form v-if="auth.isAuthenticated" class="review-form" @submit.prevent="submitReview">
            <div class="form-field">
              <label for="review-content">写下你的阅读感受</label>
              <textarea id="review-content" v-model="reviewContent" class="textarea" maxlength="1000" :aria-invalid="Boolean(reviewError)" :aria-describedby="reviewError ? 'review-error' : 'review-help'"></textarea>
              <div class="review-help-row"><p id="review-help" class="field-help">尊重他人观点，避免包含个人敏感信息。</p><span>{{ reviewContent.length }}/1000</span></div>
              <p v-if="reviewError" id="review-error" class="field-error" role="alert">{{ reviewError }}</p>
            </div>
            <button class="button" type="submit" :disabled="actionPending === 'review'">{{ actionPending === 'review' ? '正在发布…' : '发布书评' }}</button>
          </form>
          <p v-else class="sign-in-note"><RouterLink :to="{ name: 'login', query: { redirect: route.fullPath } }">登录</RouterLink>后可以评分和撰写书评。</p>

          <div v-if="reviews.length" class="review-list">
            <article v-for="review in reviews" :key="review.id" class="review-item">
              <header><strong>{{ review.nickname || review.username || '知阅读者' }}</strong><time :datetime="review.createTime">{{ formatDate(review.createTime) }}</time></header>
              <p>{{ review.content }}</p>
              <span v-if="review.likeCount" class="review-likes">{{ review.likeCount }} 人觉得有帮助</span>
            </article>
          </div>
          <p v-else class="empty-copy">还没有书评。读完后，不妨留下第一条真实感受。</p>
        </section>

        <aside class="card panel rating-panel" aria-labelledby="rating-title">
          <p class="eyebrow">YOUR RATING</p>
          <h2 id="rating-title" class="section-title">我的评分</h2>
          <template v-if="auth.isAuthenticated">
            <fieldset class="rating-options">
              <legend>选择 1 至 5 分</legend>
              <label v-for="score in 5" :key="score" :class="{ selected: rating === score }">
                <input v-model="rating" type="radio" name="rating" :value="score" />
                <span>{{ score }} 分</span>
              </label>
            </fieldset>
            <button class="button mobile-wide" type="button" :disabled="rating === 0 || actionPending === 'rating'" @click="submitRating">{{ actionPending === 'rating' ? '保存中…' : '保存评分' }}</button>
          </template>
          <p v-else class="empty-copy">登录后可以记录自己的评分。</p>
        </aside>
      </div>

      <section v-if="similar.length" class="similar-section" aria-labelledby="similar-title">
        <div class="page-heading"><div><p class="eyebrow">KEEP EXPLORING</p><h2 id="similar-title" class="section-title">同类好书</h2></div></div>
        <div class="book-grid"><BookCard v-for="item in similar" :key="item.id" :book="item" /></div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.breadcrumb { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 20px; color: var(--color-text-soft); font-size: .9rem; }
.breadcrumb a { color: var(--color-primary-strong); }
.detail-card { display: grid; gap: 24px; padding: clamp(20px, 5vw, 40px); }
.detail-cover { display: grid; width: min(100%, 250px); aspect-ratio: 4 / 5; place-items: center; overflow: hidden; border-radius: var(--radius-md); background: linear-gradient(145deg, var(--color-primary-soft), var(--color-canvas-soft)); color: var(--color-primary); font-family: var(--font-reading); font-size: 1.5rem; font-weight: 800; letter-spacing: .2em; }
.detail-cover img { width: 100%; height: 100%; object-fit: cover; }
.detail-copy { display: grid; align-content: center; justify-items: start; gap: 12px; }
.detail-copy h1 { margin: 0; font-family: var(--font-reading); font-size: clamp(2rem, 6vw, 3.7rem); line-height: 1.2; }
.detail-author { margin: 0; color: var(--color-text-soft); font-size: 1.05rem; }
.book-facts { display: flex; flex-wrap: wrap; gap: 12px 24px; margin-block: 8px; }
.book-facts span { display: grid; color: var(--color-text-muted); font-size: .78rem; }
.book-facts strong { color: var(--color-text); font-size: 1.15rem; font-variant-numeric: tabular-nums; }
.description { max-width: 68ch; margin: 0 0 8px; color: var(--color-text-soft); white-space: pre-line; }
.detail-columns { display: grid; gap: 20px; margin-top: 24px; }
.section-head { display: flex; align-items: end; justify-content: space-between; gap: 12px; margin-bottom: 20px; }
.section-head > span { color: var(--color-text-muted); }
.review-form { display: grid; justify-items: start; gap: 12px; border-bottom: 1px solid var(--color-border); padding-bottom: 24px; }
.review-help-row { display: flex; justify-content: space-between; gap: 12px; color: var(--color-text-muted); font-size: .82rem; }
.review-help-row p { margin: 0; }
.review-list { display: grid; gap: 0; }
.review-item { border-bottom: 1px solid var(--color-border); padding-block: 20px; }
.review-item:last-child { border-bottom: 0; padding-bottom: 0; }
.review-item header { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 8px; }
.review-item time { color: var(--color-text-muted); font-size: .84rem; }
.review-item p { margin: 10px 0; white-space: pre-wrap; }
.review-likes { color: var(--color-text-muted); font-size: .82rem; }
.sign-in-note { border-radius: var(--radius-sm); background: var(--color-primary-soft); padding: 12px; }
.sign-in-note a { color: var(--color-primary-strong); font-weight: 800; }
.empty-copy { color: var(--color-text-soft); }
.rating-panel { height: fit-content; }
.rating-options { display: grid; gap: 8px; margin: 18px 0; border: 0; padding: 0; }
.rating-options legend { margin-bottom: 8px; font-weight: 700; }
.rating-options label { display: flex; min-height: 44px; align-items: center; gap: 10px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); padding: 7px 12px; cursor: pointer; }
.rating-options label.selected { border-color: var(--color-primary); background: var(--color-primary-soft); }
.rating-options input { width: 18px; height: 18px; accent-color: var(--color-primary); }
.similar-section { margin-top: 48px; }
@media (min-width: 720px) { .detail-card { grid-template-columns: minmax(180px, 250px) minmax(0, 1fr); } }
@media (min-width: 960px) { .detail-columns { grid-template-columns: minmax(0, 1fr) 290px; align-items: start; } .rating-panel { position: sticky; top: 96px; } }
</style>
