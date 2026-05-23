<script setup lang="ts">
import { ref } from 'vue'
import VoiceRecorder from './components/VoiceRecorder.vue'
import type { AudioUploadVO } from './types'

const lastUpload = ref<AudioUploadVO | null>(null)
const errorMessage = ref('')

function onUploaded(result: AudioUploadVO) {
  lastUpload.value = result
  errorMessage.value = ''
}

function onError(message: string) {
  errorMessage.value = message
  window.setTimeout(() => {
    errorMessage.value = ''
  }, 6000)
}
</script>

<template>
  <main class="app-shell">
    <section class="hero">
      <div class="brand-mark" aria-hidden="true"></div>
      <div>
        <p class="eyebrow">QiNiuYun Voice Input</p>
        <h1>语音输入法</h1>
        <p class="summary">当前阶段已支持浏览器录音并上传到后端保存。</p>
      </div>
    </section>

    <VoiceRecorder @uploaded="onUploaded" @error="onError" />

    <section v-if="lastUpload" class="panel result-panel">
      <h2>最近一次上传</h2>
      <dl>
        <div>
          <dt>记录 ID</dt>
          <dd>{{ lastUpload.recordId }}</dd>
        </div>
        <div>
          <dt>文件名</dt>
          <dd>{{ lastUpload.audioName }}</dd>
        </div>
        <div>
          <dt>格式</dt>
          <dd>{{ lastUpload.audioFormat }}</dd>
        </div>
        <div>
          <dt>大小</dt>
          <dd>{{ lastUpload.audioSize }} bytes</dd>
        </div>
      </dl>
    </section>

    <p v-if="errorMessage" class="error-toast">{{ errorMessage }}</p>
  </main>
</template>
