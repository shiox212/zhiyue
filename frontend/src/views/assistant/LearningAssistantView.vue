<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { assistantApi, type AssistantAnswer, type AssistantStatus } from '@/api/assistant.api'
import { getErrorMessage } from '@/utils/format'

const status = ref<AssistantStatus | null>(null)
const loading = ref(true)
const pending = ref(false)
const question = ref('')
const asked = ref('')
const answer = ref<AssistantAnswer | null>(null)
const error = ref('')
const examples = ['怎样在 IDEA 中启动知阅？', 'JWT 过期以后怎样继续使用？', '普通用户为什么不能查看其他人的书架？']

const loadStatus = async () => {
  loading.value = true
  error.value = ''
  try { status.value = await assistantApi.status() }
  catch (cause) { error.value = getErrorMessage(cause, '暂时无法读取助手状态') }
  finally { loading.value = false }
}
const ask = async () => {
  if (pending.value || !status.value?.configured || !question.value.trim() || question.value.length > 1000) return
  pending.value = true
  error.value = ''
  answer.value = null
  asked.value = question.value.trim()
  try { answer.value = await assistantApi.ask(asked.value) }
  catch (cause) { error.value = getErrorMessage(cause, '暂时无法回答，请稍后重试') }
  finally { pending.value = false }
}
onMounted(loadStatus)
</script>

<template>
  <div class="page-shell container assistant-page">
    <header class="page-heading assistant-heading">
      <div><p class="eyebrow">LEARN WITH SOURCES</p><h1>知阅学习助手</h1><p class="lede">从项目资料出发，把每一个问题问明白。</p></div>
      <span v-if="status?.configured" class="ready-badge">DeepSeek · 已就绪</span>
    </header>

    <div class="assistant-grid">
      <section class="card question-panel" aria-labelledby="question-title">
        <p class="eyebrow">从这里开始</p><h2 id="question-title">关于知阅，你想了解什么？</h2>
        <p class="muted">可以问启动方法、登录权限、书架和推荐。回答会附上可核对的资料原文。</p>
        <div class="example-list" aria-label="示例问题">
          <button v-for="example in examples" :key="example" type="button" :disabled="pending" @click="question = example">{{ example }} <span aria-hidden="true">↗</span></button>
        </div>
        <p v-if="loading" role="status">正在连接学习助手…</p>
        <p v-else-if="status && !status.configured" class="notice" role="status">学习助手尚未配置，请联系项目维护者后再试。</p>
        <form class="stack" @submit.prevent="ask">
          <div class="form-field">
            <label for="assistant-question">你的问题</label>
            <textarea id="assistant-question" v-model="question" class="input question-input" rows="5" maxlength="1000" :disabled="pending" placeholder="例如：为什么登录后还要校验个人数据的归属？" required />
            <div class="input-hint"><span>一次提一个问题，回答更容易核对。</span><span>{{ question.length }}/1000</span></div>
          </div>
          <button class="button ask-button" type="submit" :disabled="loading || pending || !status?.configured || !question.trim()">{{ pending ? '正在查阅资料并回答…' : '向学习助手提问' }}</button>
        </form>
        <p class="privacy-note">提问会将当前问题及相关项目说明发送给 DeepSeek。请勿输入密码、密钥或个人信息。本页刷新后不保留问答。</p>
        <button v-if="!status && !loading" class="text-button" type="button" @click="loadStatus">重新连接</button>
      </section>

      <section class="answer-panel" aria-labelledby="answer-title" aria-live="polite" :aria-busy="pending">
        <div class="answer-top"><h2 id="answer-title">回答与依据</h2><span v-if="status" class="muted">{{ status.documents }} 份项目资料</span></div>
        <p v-if="error" class="notice error" role="alert">{{ error }}</p>
        <div v-if="pending" class="card empty-answer"><span class="loading-dot" aria-hidden="true"></span><h3>正在查阅项目资料</h3><p>回答可能需要几十秒，请稍候。</p></div>
        <template v-else-if="answer">
          <article class="card answer-card">
            <p class="asked">{{ asked }}</p>
            <p class="answer-label">{{ answer.status === 'ANSWERED' ? '基于资料的回答' : '本次回答说明' }}</p>
            <!-- Text interpolation only: model output is never rendered as HTML. -->
            <p class="answer-text">{{ answer.answer }}</p>
            <p class="answer-meta">{{ answer.modelCalled ? (answer.status === 'ANSWERED' ? `由 ${answer.model} 生成` : `已调用 ${answer.model} · 展示系统说明`) : '未调用模型' }} · {{ (answer.elapsedMs / 1000).toFixed(1) }} 秒</p>
          </article>
          <div v-if="answer.citations.length" class="source-list">
            <h3>核对资料</h3>
            <details v-for="(source, index) in answer.citations" :key="source.sourceId" class="card source-card">
              <summary><span class="source-number">{{ index + 1 }}</span><span>{{ source.title }}</span><span class="source-toggle">查看原文</span></summary>
              <div class="source-body"><p class="source-id">资料编号：{{ source.sourceId }}</p><blockquote>{{ source.quote }}</blockquote><p class="source-excerpt">{{ source.excerpt }}</p></div>
            </details>
          </div>
          <p class="review-note">引用已核对是否存在于项目资料，结论仍需你对照原文判断；资料不足时，请向教师或项目维护者求证。</p>
        </template>
        <div v-else-if="!error" class="card empty-answer"><div class="reading-icon" aria-hidden="true">问 · 读 · 理解</div><h3>让答案有据可查</h3><p>选择左侧的示例，或写下自己的问题。<br />回答和资料出处会一起显示在这里。</p></div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.assistant-page { display: grid; gap: 28px; }
.assistant-heading { align-items: center; }
.ready-badge { flex-shrink: 0; border-radius: 24px; background: var(--color-primary-soft); color: var(--color-primary-strong); padding: 8px 14px; font-size: .85rem; }
.assistant-grid { display: grid; gap: 28px; align-items: start; }
.question-panel, .answer-card { padding: clamp(20px, 3vw, 30px); }
.question-panel h2 { margin: 6px 0 12px; font-family: var(--font-reading); font-size: 1.45rem; }
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
