<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import StatusPanel from '@/components/base/StatusPanel.vue'
import { shelfApi } from '@/api/shelf.api'
import { useAuthStore } from '@/stores/auth.store'
import { useUiStore } from '@/stores/ui.store'
import type { ShelfItem } from '@/types/domain'
import { getErrorMessage } from '@/utils/format'

const auth = useAuthStore()
const ui = useUiStore()
const items = ref<ShelfItem[]>([])
const selectedStatus = ref(-1)
const loading = ref(true)
const pendingId = ref<number | null>(null)
const error = ref('')

const statuses = [
  { value: -1, label: '全部' },
  { value: 0, label: '想读' },
  { value: 1, label: '在读' },
  { value: 2, label: '已读' },
]

const visibleItems = computed(() => selectedStatus.value < 0 ? items.value : items.value.filter((item) => item.readStatus === selectedStatus.value))
const count = (status: number) => status < 0 ? items.value.length : items.value.filter((item) => item.readStatus === status).length

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    items.value = await shelfApi.list(auth.user!.id)
  } catch (cause) {
    error.value = getErrorMessage(cause, '书架暂时无法加载')
  } finally {
    loading.value = false
  }
}

const updateStatus = async (item: ShelfItem, event: Event) => {
  const value = Number((event.target as HTMLSelectElement).value)
  const previous = item.readStatus ?? 0
  pendingId.value = item.id
  item.readStatus = value
  try {
    await shelfApi.save(auth.user!.id, item.id, value)
    ui.notify('阅读状态已更新', 'success')
  } catch (cause) {
    item.readStatus = previous
    ui.notify(getErrorMessage(cause), 'error')
  } finally {
    pendingId.value = null
  }
}

const remove = async (item: ShelfItem) => {
  if (!window.confirm(`确定将《${item.title}》移出书架吗？`)) return
  pendingId.value = item.id
  try {
    await shelfApi.remove(auth.user!.id, item.id)
    items.value = items.value.filter((entry) => entry.id !== item.id)
    ui.notify('已移出书架', 'success')
  } catch (cause) {
    ui.notify(getErrorMessage(cause), 'error')
  } finally {
    pendingId.value = null
  }
}

onMounted(load)
</script>

<template>
  <div class="page-shell container">
    <header class="page-heading">
      <div><p class="eyebrow">MY READING</p><h1>我的书架</h1><p class="lede">把想读、在读与读完的书放在一个清晰的地方。</p></div>
      <RouterLink to="/books" class="button secondary">继续找书</RouterLink>
    </header>

    <div class="status-tabs" role="group" aria-label="按阅读状态筛选">
      <button v-for="status in statuses" :key="status.value" type="button" :aria-pressed="selectedStatus === status.value" @click="selectedStatus = status.value">
        {{ status.label }} <span>{{ count(status.value) }}</span>
      </button>
    </div>

    <StatusPanel v-if="loading" tone="loading" title="正在整理书架" />
    <StatusPanel v-else-if="error" tone="error" title="书架加载失败" :description="error"><button class="button secondary" type="button" @click="load">重新加载</button></StatusPanel>
    <StatusPanel v-else-if="visibleItems.length === 0" tone="empty" title="这里还没有图书" :description="selectedStatus < 0 ? '从发现页加入一本感兴趣的书吧。' : '这个阅读状态下还没有图书。'">
      <RouterLink to="/books" class="button">浏览图书</RouterLink>
    </StatusPanel>

    <div v-else class="shelf-grid">
      <article v-for="item in visibleItems" :key="item.id" class="shelf-card card">
        <RouterLink :to="`/books/${item.id}`" class="shelf-card__cover" :aria-label="`查看《${item.title}》详情`">
          <img v-if="item.coverUrl" :src="item.coverUrl" :alt="`《${item.title}》封面`" loading="lazy" />
          <span v-else aria-hidden="true">知阅</span>
        </RouterLink>
        <div class="shelf-card__body">
          <span class="tag">{{ item.category }}</span>
          <h2><RouterLink :to="`/books/${item.id}`">{{ item.title }}</RouterLink></h2>
          <p>{{ item.author }}</p>
          <div class="form-field">
            <label :for="`status-${item.id}`">阅读状态</label>
            <select :id="`status-${item.id}`" class="select" :value="item.readStatus ?? 0" :disabled="pendingId === item.id" @change="updateStatus(item, $event)">
              <option :value="0">想读</option><option :value="1">在读</option><option :value="2">已读</option>
            </select>
          </div>
          <button type="button" class="text-button remove-button" :disabled="pendingId === item.id" @click="remove(item)">移出书架</button>
        </div>
      </article>
    </div>
  </div>
</template>

<style scoped>
.status-tabs { display: flex; gap: 8px; margin-bottom: 24px; overflow-x: auto; padding-bottom: 4px; }
.status-tabs button { display: inline-flex; min-width: max-content; min-height: 44px; align-items: center; gap: 8px; border: 1px solid var(--color-border); border-radius: 999px; background: var(--color-surface); padding: 8px 14px; font-weight: 700; }
.status-tabs button[aria-pressed='true'] { border-color: var(--color-primary); background: var(--color-primary); color: white; }
.status-tabs span { font-variant-numeric: tabular-nums; opacity: .75; }
.shelf-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 290px), 1fr)); gap: 18px; }
.shelf-card { display: grid; grid-template-columns: 112px minmax(0, 1fr); overflow: hidden; }
.shelf-card__cover { display: grid; min-height: 190px; place-items: center; background: linear-gradient(145deg, var(--color-primary-soft), var(--color-canvas-soft)); color: var(--color-primary); font-family: var(--font-reading); font-weight: 800; letter-spacing: .18em; text-decoration: none; }
.shelf-card__cover img { width: 100%; height: 100%; object-fit: contain; }
.shelf-card__body { display: grid; align-content: start; gap: 8px; padding: 16px; }
.shelf-card h2 { margin: 0; font-family: var(--font-reading); font-size: 1.1rem; line-height: 1.35; }
.shelf-card h2 a { text-decoration: none; }
.shelf-card p { margin: 0; color: var(--color-text-soft); font-size: .88rem; }
.shelf-card .form-field { margin-top: 6px; }
.shelf-card .select { min-height: 44px; }
.remove-button { width: fit-content; margin-left: -12px; color: var(--color-danger); }
@media (max-width: 390px) { .shelf-card { grid-template-columns: 96px minmax(0, 1fr); } }
</style>
