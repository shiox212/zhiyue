<script setup lang="ts">
import { onBeforeUnmount, reactive, ref, useId, watch } from 'vue'
import { assistantApi, type AssistantAnswer, type AssistantStatus } from '@/api/assistant.api'
import { getErrorMessage } from '@/utils/format'
import { RouterLink, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'

defineProps<{ compact?: boolean }>()
const fieldId = useId()
const auth = useAuthStore()
const route = useRoute()
const mode = ref<'LIBRARY' | 'GENERAL'>('LIBRARY')

const status = ref<AssistantStatus | null>(null)
const loading = ref(false)
const pending = ref(false)
const question = ref('')
const error = ref('')
interface Turn {
  id: number
  question: string
  mode: 'LIBRARY' | 'GENERAL'
  pending: boolean
  answer: AssistantAnswer | null
  error: string
}
const history = ref<Turn[]>([])
let sessionVersion = 0
let turnId = 0
const examples = ['站内有哪些余华的作品？', '《三体》的作者和简介是什么？', '有没有科幻类图书？']

const loadStatus = async () => {
  if (!auth.isAuthenticated) return
  const version = sessionVersion
  loading.value = true
  error.value = ''
  try {
    const result = await assistantApi.status()
    if (version === sessionVersion) status.value = result
  }
  catch (cause) { if (version === sessionVersion) error.value = getErrorMessage(cause, '暂时无法读取助手状态') }
  finally { if (version === sessionVersion) loading.value = false }
}
const ask = async () => {
  if (!auth.isAuthenticated || pending.value || !status.value?.configured || !question.value.trim() || question.value.length > 1000) return
  const version = sessionVersion
  const turn = reactive<Turn>({ id: ++turnId, question: question.value.trim(), mode: mode.value, pending: true, answer: null, error: '' })
  history.value.push(turn)
  pending.value = true
  error.value = ''
  try {
    const result = await assistantApi.ask(turn.question, turn.mode)
    if (version === sessionVersion) turn.answer = result
  }
  catch (cause) { if (version === sessionVersion) turn.error = getErrorMessage(cause, '暂时无法回答，请稍后重试') }
  finally {
    if (version === sessionVersion) { turn.pending = false; pending.value = false }
  }
}
// Do not retain one reader's history or late responses when their identity changes.
watch(() => auth.user?.id, () => {
  sessionVersion++
  history.value = []
  status.value = null
  question.value = ''
  mode.value = 'LIBRARY'
  pending.value = false
  loading.value = false
  error.value = ''
  if (auth.isAuthenticated) void loadStatus()
}, { immediate: true, flush: 'sync' })
onBeforeUnmount(() => { sessionVersion++ })
</script>

<template>
  <div class="assistant-page" :class="{ compact }">
    <header class="page-heading assistant-heading">
      <div><p class="eyebrow">READ WITH SOURCES</p><h2>图书知识助手</h2><p class="lede">查阅站内图书，了解文学与阅读。</p></div>
      <span v-if="status?.configured" class="ready-badge">DeepSeek · 已就绪</span>
    </header>

    <div v-if="!auth.isAuthenticated" class="card login-notice" role="status">
      <p>登录后可向图书知识助手提问。</p>
      <RouterLink class="button" :to="{ path: '/login', query: { redirect: route.fullPath } }">登录后提问</RouterLink>
    </div>

    <div v-else class="assistant-grid">
      <section class="card question-panel" :aria-labelledby="`${fieldId}-title`">
        <p class="eyebrow">从这里开始</p><h3 :id="`${fieldId}-title`">想了解哪本书？</h3>
        <p class="muted">站内模式核对书名、作者、分类和简介；通用知识模式解释文学背景、概念并提供阅读建议。</p>
        <div class="example-list" aria-label="示例问题">
          <button v-for="example in examples" :key="example" type="button" :disabled="pending" @click="question = example">{{ example }} <span aria-hidden="true">↗</span></button>
        </div>
        <p v-if="loading" role="status">正在连接图书知识助手…</p>
        <p v-else-if="status && !status.configured" class="notice" role="status">学习助手尚未配置，请联系项目维护者后再试。</p>
        <form class="stack" @submit.prevent="ask">
          <div class="form-field">
            <label :for="`${fieldId}-mode`">回答模式</label>
            <select :id="`${fieldId}-mode`" v-model="mode" class="input" :disabled="pending">
              <option value="LIBRARY">站内图书与资料 · 提供引用</option>
              <option value="GENERAL">模型通用知识 · 非站内资料</option>
            </select>
            <p class="mode-note">{{ mode === 'GENERAL' ? '模型通用知识不代表站内收录，也不是实时联网结果；不会提供站内引用。' : '只依据站内公开图书信息和已收录项目资料；资料不足时不调用模型。' }}</p>
          </div>
          <div class="form-field">
            <label :for="`${fieldId}-question`">你的问题</label>
            <textarea :id="`${fieldId}-question`" v-model="question" class="input question-input" rows="4" maxlength="1000" :disabled="pending" :placeholder="mode === 'GENERAL' ? '例如：什么是魔幻现实主义？' : '例如：《活着》的作者和站内简介是什么？'" required />
            <div class="input-hint"><span>一次提一个问题，回答更容易核对。</span><span>{{ question.length }}/1000</span></div>
          </div>
          <button class="button ask-button" type="submit" :disabled="loading || pending || !status?.configured || !question.trim()">{{ pending ? '正在回答…' : '向图书知识助手提问' }}</button>
        </form>
        <p class="privacy-note">当前问题和少量命中资料会发送给 DeepSeek；通用知识模式只发送问题。请勿输入密码、密钥或个人信息。问答仅在当前页面会话中保留，刷新或退出登录后清空。</p>
        <button v-if="!status && !loading" class="text-button" type="button" @click="loadStatus">重新连接</button>
      </section>

      <section class="answer-panel" :aria-labelledby="`${fieldId}-answer`" aria-live="polite" :aria-busy="pending">
        <div class="answer-top"><h3 :id="`${fieldId}-answer`">回答与依据</h3></div>
        <p v-if="error" class="notice error" role="alert">{{ error }}</p>
        <div v-for="turn in history" :key="turn.id" class="conversation-turn">
          <p class="turn-question">{{ turn.question }}</p>
          <p class="turn-mode">{{ turn.mode === 'GENERAL' ? '本次模式：模型通用知识 · 非站内资料 · 非实时联网' : '本次模式：站内图书与资料' }}</p>
          <div v-if="turn.pending" class="card empty-answer"><span class="loading-dot" aria-hidden="true"></span><h3>正在准备回答</h3><p>回答可能需要几十秒，请稍候。</p></div>
          <p v-else-if="turn.error" class="notice error" role="alert">本次提问未完成：{{ turn.error }}</p>
          <template v-else-if="turn.answer">
          <article class="card answer-card">
            <p class="answer-label">{{ turn.answer.status === 'GENERAL_KNOWLEDGE' ? '模型通用知识 · 非站内资料 · 非实时联网' : turn.answer.status === 'ANSWERED' ? '基于站内资料的回答' : '本次回答说明' }}</p>
            <!-- Text interpolation only: model output is never rendered as HTML. -->
            <p class="answer-text">{{ turn.answer.answer }}</p>
            <p class="answer-meta">{{ turn.answer.modelCalled ? (['ANSWERED', 'GENERAL_KNOWLEDGE'].includes(turn.answer.status) ? `由 ${turn.answer.model} 生成` : `已调用 ${turn.answer.model} · 展示系统说明`) : '未调用模型' }} · {{ (turn.answer.elapsedMs / 1000).toFixed(1) }} 秒</p>
          </article>
          <div v-if="turn.answer.citations.length" class="source-list">
            <h3>核对资料</h3>
            <details v-for="(source, index) in turn.answer.citations" :key="source.sourceId" class="card source-card">
              <summary><span class="source-number">{{ index + 1 }}</span><span>{{ source.title }}</span><span class="source-toggle">查看原文</span></summary>
              <div class="source-body"><p class="source-id">{{ source.bookId ? '站内图书' : '项目资料' }} · 资料编号：{{ source.sourceId }}</p><blockquote>{{ source.quote }}</blockquote><p class="source-excerpt">{{ source.excerpt }}</p><RouterLink v-if="source.bookId && Number.isInteger(source.bookId) && source.bookId > 0" class="book-link" :to="`/books/${source.bookId}`">查看图书详情：{{ source.title }}</RouterLink></div>
            </details>
          </div>
          <p class="review-note">{{ turn.answer.status === 'GENERAL_KNOWLEDGE' ? '通用知识来自模型，可能有误，请结合可靠资料核对；阅读建议仅供参考。' : '引用已核对是否存在于站内资料，结论仍需对照原文判断。简介不等于图书全文。' }}</p>
          </template>
        </div>
        <div v-if="!history.length && !error" class="card empty-answer"><div class="reading-icon" aria-hidden="true">问 · 读 · 理解</div><h3>让答案有据可查</h3><p>选择左侧的示例，或写下自己的问题。<br />回答和资料出处会一起显示在这里。</p></div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.assistant-page { display: grid; gap: 28px; }
.login-notice { padding: 24px; }
.conversation-turn { margin-bottom: 24px; }
.turn-question { font-weight: 700; white-space: pre-wrap; overflow-wrap: anywhere; }
.turn-mode { color: var(--color-text-soft); font-size: .85rem; line-height: 1.7; }
.assistant-heading { align-items: center; }
.ready-badge { flex-shrink: 0; border-radius: 24px; background: var(--color-primary-soft); color: var(--color-primary-strong); padding: 8px 14px; font-size: .85rem; }
.assistant-grid { display: grid; gap: 28px; align-items: start; }
.question-panel, .answer-card { padding: clamp(20px, 3vw, 30px); }
.question-panel h3 { margin: 6px 0 12px; font-family: var(--font-reading); font-size: 1.45rem; }
.mode-note { margin: 0; color: var(--color-text-soft); font-size: .85rem; line-height: 1.7; }
.book-link { display: inline-flex; align-items: center; min-height: 44px; margin-top: 12px; color: var(--color-primary-strong); }
.compact .assistant-grid { grid-template-columns: minmax(0, 1fr); gap: 18px; }
.compact .empty-answer { min-height: 150px; }
.compact .assistant-heading { display: none; }
.compact .example-list { margin-block: 14px; }
.compact .question-panel, .compact .answer-card { padding: 18px; }
.muted, .privacy-note, .review-note, .answer-meta { color: var(--color-text-soft); }
.example-list { display: grid; gap: 8px; margin-block: 24px; }
.example-list button { display: flex; justify-content: space-between; gap: 10px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-canvas); padding: 12px 14px; text-align: left; color: var(--color-primary-strong); font: inherit; font-size: .87rem; cursor: pointer; }
.example-list button:hover { background: var(--color-primary-soft); }
.question-input { resize: vertical; min-height: 140px; line-height: 1.7; }
.input-hint { display: flex; justify-content: space-between; gap: 8px; font-size: .75rem; color: var(--color-text-muted); }
.ask-button { width: 100%; }
.privacy-note { margin: 16px 0 0; font-size: .78rem; line-height: 1.7; }
.answer-top { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 16px; }
.answer-top h2, .source-list h3 { margin: 0; font-family: var(--font-reading); }
.answer-top span { font-size: .8rem; }
.empty-answer { min-height: 360px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10px; padding: 28px; text-align: center; color: var(--color-text-soft); border-style: dashed; }
.empty-answer h3 { margin: 6px 0 0; color: var(--color-text); font-family: var(--font-reading); font-size: 1.45rem; }
.empty-answer p { margin: 0; line-height: 1.8; }
.reading-icon { padding: 18px 24px; background: var(--color-primary-soft); border-radius: var(--radius-md); letter-spacing: .15em; font-family: var(--font-reading); color: var(--color-primary-strong); }
.loading-dot { width: 16px; height: 16px; border-radius: 50%; background: var(--color-primary); }
.notice { border-left: 3px solid var(--color-accent); padding: 14px; background: var(--color-accent-soft); border-radius: 4px; }
.error { background: var(--color-danger-soft); border-color: var(--color-danger); color: var(--color-danger); }
.asked { border-bottom: 1px solid var(--color-border); margin: 0 0 20px; padding-bottom: 16px; font-weight: 700; overflow-wrap: anywhere; }
.answer-label { margin: 0 0 10px; color: var(--color-primary); font-size: .8rem; font-weight: 700; }
.answer-text { white-space: pre-wrap; line-height: 1.95; margin: 0; overflow-wrap: anywhere; }
.answer-meta { margin: 20px 0 0; font-size: .75rem; }
.source-list { display: grid; gap: 10px; margin-top: 24px; }
.source-list h3 { font-size: 1.1rem; margin-bottom: 2px; }
.source-card summary { display: flex; align-items: center; gap: 10px; padding: 15px; cursor: pointer; font-size: .88rem; }
.source-number { display: grid; place-items: center; width: 25px; height: 25px; flex-shrink: 0; border-radius: 50%; background: var(--color-primary-soft); color: var(--color-primary-strong); }
.source-toggle { margin-left: auto; font-size: .75rem; color: var(--color-text-soft); white-space: nowrap; }
.source-body { border-top: 1px solid var(--color-border); padding: 16px; font-size: .86rem; line-height: 1.8; overflow-wrap: anywhere; }
.source-id { margin: 0; color: var(--color-text-muted); font-size: .74rem; }
.source-body blockquote { border-left: 3px solid var(--color-primary); padding: 6px 12px; margin: 14px 0; background: var(--color-primary-soft); }
.source-excerpt { margin: 0; white-space: pre-wrap; }
.review-note { font-size: .78rem; line-height: 1.8; padding-inline: 4px; }
@media (min-width: 920px) { .assistant-grid { grid-template-columns: minmax(0, .85fr) minmax(0, 1.15fr); } }
</style>
