<script setup lang="ts">
import { onUnmounted, ref } from 'vue'
import { uploadAudio } from '../api'
import type { AudioUploadVO } from '../types'

const emit = defineEmits<{
  uploaded: [result: AudioUploadVO]
  error: [message: string]
}>()

const recording = ref(false)
const uploading = ref(false)
const audioLevel = ref(0)

let mediaRecorder: MediaRecorder | null = null
let mediaStream: MediaStream | null = null
let analyser: AnalyserNode | null = null
let animationId = 0
let chunks: Blob[] = []

async function startRecording() {
  try {
    chunks = []
    mediaStream = await navigator.mediaDevices.getUserMedia({
      audio: {
        channelCount: 1,
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true,
      },
    })

    const mimeType = getSupportedMimeType()
    mediaRecorder = new MediaRecorder(mediaStream, mimeType ? { mimeType } : undefined)

    mediaRecorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        chunks.push(event.data)
      }
    }

    mediaRecorder.onstop = async () => {
      stopAudioLevelMonitor()
      stopAudioStream()
      if (chunks.length === 0) return

      const audio = new Blob(chunks, { type: mimeType || 'audio/webm' })
      await doUpload(audio)
    }

    mediaRecorder.start(250)
    recording.value = true
    startAudioLevelMonitor()
  } catch (error) {
    emit('error', getMicrophoneErrorMessage(error))
  }
}

function stopRecording() {
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
  }
  recording.value = false
}

async function doUpload(audio: Blob) {
  uploading.value = true
  try {
    const response = await uploadAudio(audio)
    emit('uploaded', response.data)
  } catch (error) {
    const message = error instanceof Error ? error.message : '录音上传失败'
    emit('error', message)
  } finally {
    uploading.value = false
  }
}

function toggleRecording() {
  if (uploading.value) return
  if (recording.value) {
    stopRecording()
  } else {
    startRecording()
  }
}

function getSupportedMimeType(): string | undefined {
  const types = [
    'audio/webm;codecs=opus',
    'audio/webm',
    'audio/ogg;codecs=opus',
  ]
  return types.find((type) => MediaRecorder.isTypeSupported(type))
}

function startAudioLevelMonitor() {
  if (!mediaStream) return
  const context = new AudioContext()
  analyser = context.createAnalyser()
  analyser.fftSize = 256
  const source = context.createMediaStreamSource(mediaStream)
  source.connect(analyser)
  const data = new Uint8Array(analyser.frequencyBinCount)

  function tick() {
    if (!analyser) return
    analyser.getByteFrequencyData(data)
    const average = data.reduce((sum, value) => sum + value, 0) / data.length
    audioLevel.value = Math.min(average / 128, 1)
    animationId = requestAnimationFrame(tick)
  }

  tick()
}

function stopAudioLevelMonitor() {
  if (animationId) {
    cancelAnimationFrame(animationId)
    animationId = 0
  }
  analyser = null
}

function stopAudioStream() {
  if (mediaStream) {
    mediaStream.getTracks().forEach((track) => track.stop())
    mediaStream = null
  }
}

function getMicrophoneErrorMessage(error: unknown): string {
  if (error instanceof DOMException) {
    if (error.name === 'NotAllowedError' || error.name === 'PermissionDeniedError') {
      return '麦克风权限被拒绝，请允许浏览器使用麦克风后重试'
    }
    if (error.name === 'NotFoundError') {
      return '未检测到麦克风设备'
    }
    if (error.name === 'NotReadableError') {
      return '麦克风可能被其他应用占用'
    }
  }
  return '无法访问麦克风'
}

onUnmounted(() => {
  if (recording.value) {
    stopRecording()
  }
  stopAudioLevelMonitor()
  stopAudioStream()
})
</script>

<template>
  <section class="recorder-panel">
    <button
      class="record-button"
      :class="{ recording, uploading }"
      :disabled="uploading"
      @click="toggleRecording"
    >
      <span v-if="uploading" class="spinner"></span>
      <span v-else-if="recording" class="stop-icon"></span>
      <span v-else class="mic-icon">🎤</span>
    </button>

    <div class="recorder-copy">
      <strong v-if="uploading">上传中...</strong>
      <strong v-else-if="recording">录音中...</strong>
      <strong v-else>点击开始录音</strong>
      <span>本 PR 会把录音上传到后端保存，识别能力将在后续 PR 接入。</span>
    </div>

    <div v-if="recording" class="level-track">
      <div class="level-fill" :style="{ width: `${Math.round(audioLevel * 100)}%` }"></div>
    </div>
  </section>
</template>
