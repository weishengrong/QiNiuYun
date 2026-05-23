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

let mediaStream: MediaStream | null = null
let audioContext: AudioContext | null = null
let sourceNode: MediaStreamAudioSourceNode | null = null
let processorNode: ScriptProcessorNode | null = null
let analyser: AnalyserNode | null = null
let animationId = 0
let pcmChunks: Int16Array[] = []

async function startRecording() {
  try {
    pcmChunks = []
    mediaStream = await navigator.mediaDevices.getUserMedia({
      audio: {
        channelCount: 1,
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true,
      },
    })

    recording.value = true
    await startPcmRecorder()
    startAudioLevelMonitor()
  } catch (error) {
    emit('error', getMicrophoneErrorMessage(error))
  }
}

async function startPcmRecorder() {
  if (!mediaStream) return

  audioContext = new AudioContext()
  sourceNode = audioContext.createMediaStreamSource(mediaStream)
  processorNode = audioContext.createScriptProcessor(4096, 1, 1)

  processorNode.onaudioprocess = (event) => {
    if (!recording.value || !audioContext) return
    const input = event.inputBuffer.getChannelData(0)
    pcmChunks.push(resampleTo16kPcm(input, audioContext.sampleRate))
  }

  sourceNode.connect(processorNode)
  processorNode.connect(audioContext.destination)
}

function stopRecording() {
  recording.value = false
  stopPcmRecorder()
  stopAudioLevelMonitor()
  stopAudioStream()

  if (pcmChunks.length > 0) {
    const wavBlob = pcmChunksToWavBlob(pcmChunks)
    pcmChunks = []
    doUpload(wavBlob)
  }
}

function stopPcmRecorder() {
  if (processorNode) {
    processorNode.disconnect()
    processorNode.onaudioprocess = null
    processorNode = null
  }
  if (sourceNode) {
    sourceNode.disconnect()
    sourceNode = null
  }
  if (audioContext) {
    audioContext.close()
    audioContext = null
  }
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

function resampleTo16kPcm(input: Float32Array, sourceRate: number): Int16Array {
  const targetRate = 16000
  const ratio = sourceRate / targetRate
  const outputLength = Math.floor(input.length / ratio)
  const output = new Int16Array(outputLength)

  for (let i = 0; i < outputLength; i++) {
    const sourceIndex = Math.floor(i * ratio)
    const sample = Math.max(-1, Math.min(1, input[sourceIndex] ?? 0))
    output[i] = sample < 0 ? sample * 0x8000 : sample * 0x7fff
  }

  return output
}

function pcmChunksToWavBlob(chunks: Int16Array[]): Blob {
  const totalSamples = chunks.reduce((sum, chunk) => sum + chunk.length, 0)
  const dataSize = totalSamples * 2
  const buffer = new ArrayBuffer(44 + dataSize)
  const view = new DataView(buffer)

  writeString(view, 0, 'RIFF')
  view.setUint32(4, 36 + dataSize, true)
  writeString(view, 8, 'WAVE')
  writeString(view, 12, 'fmt ')
  view.setUint32(16, 16, true)
  view.setUint16(20, 1, true)
  view.setUint16(22, 1, true)
  view.setUint32(24, 16000, true)
  view.setUint32(28, 16000 * 2, true)
  view.setUint16(32, 2, true)
  view.setUint16(34, 16, true)
  writeString(view, 36, 'data')
  view.setUint32(40, dataSize, true)

  let offset = 44
  for (const chunk of chunks) {
    for (let i = 0; i < chunk.length; i++) {
      view.setInt16(offset, chunk[i], true)
      offset += 2
    }
  }

  return new Blob([buffer], { type: 'audio/wav' })
}

function writeString(view: DataView, offset: number, text: string) {
  for (let i = 0; i < text.length; i++) {
    view.setUint8(offset + i, text.charCodeAt(i))
  }
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
  stopPcmRecorder()
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
      <span>录音会转换为 16kHz / 16bit / 单声道 WAV，以兼容 Vosk 离线识别。</span>
    </div>

    <div v-if="recording" class="level-track">
      <div class="level-fill" :style="{ width: `${Math.round(audioLevel * 100)}%` }"></div>
    </div>
  </section>
</template>
