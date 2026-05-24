<script setup lang="ts">
import { ref } from 'vue'
import HistoryRecords from './components/HistoryRecords.vue'
import TextEditor from './components/TextEditor.vue'
import VoiceRecorder from './components/VoiceRecorder.vue'
import type { EngineOption } from './types'

const text = ref('')
const liveDraft = ref('')
const errorMessage = ref('')
const engine = ref('stepfun_stream')

const engines: EngineOption[] = [
  { label: 'StepFun 实时', value: 'stepfun_stream', mode: 'stream' },
  { label: 'Vosk 离线', value: 'vosk', mode: 'upload' },
]

function onRecognized(result: string) {
  text.value = result
  liveDraft.value = ''
  errorMessage.value = ''
}

function onLiveDelta(result: string) {
  liveDraft.value = result
}

function onLiveCompleted(result: string) {
  const separator = text.value && !/[，。！？；：,.!?;\s]$/.test(text.value) ? ' ' : ''
  text.value = `${text.value}${separator}${result}`
  liveDraft.value = ''
  errorMessage.value = ''
}

function onError(message: string) {
  errorMessage.value = message
  window.setTimeout(() => {
    errorMessage.value = ''
  }, 6000)
}

function onReuse(result: string) {
  text.value = result
  liveDraft.value = ''
}

function getEngineMode(value: string): 'stream' | 'upload' {
  return engines.find((item) => item.value === value)?.mode ?? 'upload'
}
</script>

<template>
  <main class="app-shell">
    <header class="hero">
      <div class="brand-mark" aria-hidden="true"></div>
      <div>
        <p class="eyebrow">QiNiuYun Voice Input</p>
        <h1>语音输入法</h1>
        <p class="summary">支持 StepFun 实时语音输入和 Vosk 离线识别。</p>
      </div>
    </header>

    <section class="metrics-row" aria-label="产品能力">
      <div>
        <strong>实时</strong>
        <span>边说边出字</span>
      </div>
      <div>
        <strong>准确</strong>
        <span>最终文本回填</span>
      </div>
      <div>
        <strong>可控</strong>
        <span>云端实时 + 离线回退</span>
      </div>
    </section>

    <section class="toolbar-panel">
      <label for="engine">识别引擎</label>
      <select id="engine" v-model="engine">
        <option v-for="item in engines" :key="item.value" :value="item.value">
          {{ item.label }}
        </option>
      </select>
    </section>

    <TextEditor
      :text="liveDraft ? `${text}${text ? ' ' : ''}${liveDraft}` : text"
      @update-text="text = $event"
    />

    <VoiceRecorder
      :engine="engine"
      :mode="getEngineMode(engine)"
      @recognized="onRecognized"
      @live-delta="onLiveDelta"
      @live-completed="onLiveCompleted"
      @error="onError"
    />

    <HistoryRecords @reuse="onReuse" @error="onError" />

    <p v-if="errorMessage" class="error-toast">{{ errorMessage }}</p>
  </main>
</template>
