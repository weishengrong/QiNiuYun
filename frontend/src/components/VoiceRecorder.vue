<script setup lang="ts">
import { onUnmounted, ref } from 'vue'
import { recognizeAudio } from '../api'

const emit = defineEmits<{
  recognized: [text: string]
  liveDelta: [text: string]
  liveCompleted: [text: string]
  error: [message: string]
}>()

const props = defineProps<{
  engine: string
  mode: 'stream' | 'upload'
}>()

const recording = ref(false)
const uploading = ref(false)
const audioLevel = ref(0)
const streamStatus = ref<'idle' | 'connecting' | 'listening' | 'speaking'>('idle')
const streamMessage = ref('')

let mediaStream: MediaStream | null = null
let audioContext: AudioContext | null = null
let sourceNode: MediaStreamAudioSourceNode | null = null
let processorNode: ScriptProcessorNode | null = null
let analyser: AnalyserNode | null = null
let animationId = 0
let pcmChunks: Int16Array[] = []
let streamSocket: WebSocket | null = null
let pendingPcm: Int16Array[] = []
let streamReady = false
let partialText = ''

async function startRecording() {
  if (props.mode === 'stream') {
    await startStreaming()
    return
  }
  await startOfflineRecording()
}

async function startOfflineRecording() {
  try {
    pcmChunks = []
    mediaStream = await getMicrophoneStream()
    recording.value = true
    await startPcmRecorder((pcm) => pcmChunks.push(pcm))
    startAudioLevelMonitor()
  } catch (error) {
    emit('error', getMicrophoneErrorMessage(error))
  }
}

async function startStreaming() {
  try {
    resetStreamingState()
    pendingPcm = []
    partialText = ''
    streamStatus.value = 'connecting'
    streamMessage.value = '连接实时识别...'
    mediaStream = await getMicrophoneStream()

    streamSocket = new WebSocket(getStepFunWsUrl())
    streamSocket.onopen = () => {
      recording.value = true
      streamStatus.value = 'listening'
      streamMessage.value = '等待 StepFun 配置...'
    }
    streamSocket.onmessage = (event) => handleStreamMessage(event.data)
    streamSocket.onerror = () => {
      streamMessage.value = '实时识别连接异常'
      emit('error', '实时识别连接异常，请检查后端服务和 StepFun Token 配置')
    }
    streamSocket.onclose = () => {
      recording.value = false
      streamStatus.value = 'idle'
      if (streamMessage.value && streamMessage.value !== '实时识别连接异常') {
        streamMessage.value = '实时识别已关闭'
      }
    }

    await startPcmRecorder(sendPcmChunk)
    startAudioLevelMonitor()
  } catch (error) {
    stopStreaming()
    emit('error', getMicrophoneErrorMessage(error))
  }
}

async function getMicrophoneStream(): Promise<MediaStream> {
  return navigator.mediaDevices.getUserMedia({
    audio: {
      channelCount: 1,
      echoCancellation: true,
      noiseSuppression: true,
      autoGainControl: true,
    },
  })
}

async function startPcmRecorder(onPcm: (pcm: Int16Array) => void) {
  if (!mediaStream) return

  audioContext = new AudioContext()
  sourceNode = audioContext.createMediaStreamSource(mediaStream)
  processorNode = audioContext.createScriptProcessor(4096, 1, 1)

  processorNode.onaudioprocess = (event) => {
    if (!recording.value && props.mode !== 'stream') return
    if (!audioContext) return
    const input = event.inputBuffer.getChannelData(0)
    onPcm(resampleTo16kPcm(input, audioContext.sampleRate))
  }

  sourceNode.connect(processorNode)
  processorNode.connect(audioContext.destination)
}

function stopRecording() {
  if (props.mode === 'stream') {
    stopStreaming()
    return
  }

  recording.value = false
  stopPcmRecorder()
  stopAudioLevelMonitor()
  stopAudioStream()

  if (pcmChunks.length > 0) {
    const wavBlob = pcmChunksToWavBlob(pcmChunks)
    pcmChunks = []
    doRecognize(wavBlob)
  }
}

function stopStreaming() {
  recording.value = false
  streamStatus.value = 'idle'
  streamReady = false
  streamMessage.value = ''
  stopPcmRecorder()
  stopAudioLevelMonitor()
  stopAudioStream()

  if (streamSocket && streamSocket.readyState === WebSocket.OPEN) {
    streamSocket.send(JSON.stringify({ type: 'session.stop' }))
    streamSocket.close()
  } else if (streamSocket) {
    streamSocket.close()
  }
  streamSocket = null
  pendingPcm = []
  partialText = ''
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

async function doRecognize(audio: Blob) {
  uploading.value = true
  try {
    const response = await recognizeAudio(audio, props.engine)
    emit('recognized', response.data.originalText)
  } catch (error) {
    const message = error instanceof Error ? error.message : '离线识别失败'
    emit('error', message)
  } finally {
    uploading.value = false
  }
}

function handleStreamMessage(raw: string) {
  const message = JSON.parse(raw) as { type: string; text?: string; message?: string }
  if (message.type === 'ready') {
    streamMessage.value = '实时通道已连接...'
  } else if (message.type === 'configured') {
    streamReady = true
    streamMessage.value = '正在聆听...'
    flushPendingPcm()
  } else if (message.type === 'speech_started') {
    streamStatus.value = 'speaking'
    streamMessage.value = '正在转写...'
  } else if (message.type === 'speech_stopped') {
    streamStatus.value = 'listening'
    streamMessage.value = '正在整理句子...'
  } else if (message.type === 'delta') {
    partialText += message.text ?? ''
    emit('liveDelta', partialText)
  } else if (message.type === 'completed') {
    const completedText = (message.text || partialText).trim()
    if (completedText) {
      emit('liveCompleted', completedText)
    }
    partialText = ''
    streamStatus.value = 'listening'
    streamMessage.value = '正在聆听...'
    emit('liveDelta', '')
  } else if (message.type === 'error') {
    streamMessage.value = '实时识别异常'
    emit('error', message.message || '实时识别服务异常')
  } else if (message.type === 'closed') {
    streamMessage.value = message.message || '实时识别已关闭'
  }
}

function sendPcmChunk(pcm: Int16Array) {
  if (!pcm.length) return
  if (!streamSocket || streamSocket.readyState !== WebSocket.OPEN || !streamReady) {
    pendingPcm.push(pcm)
    return
  }
  streamSocket.send(JSON.stringify({
    type: 'audio.append',
    audio: pcmToBase64(pcm),
  }))
}

function flushPendingPcm() {
  const chunks = pendingPcm.splice(0)
  chunks.forEach(sendPcmChunk)
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

function pcmToBase64(pcm: Int16Array): string {
  const bytes = new Uint8Array(pcm.buffer)
  let binary = ''
  const batchSize = 0x8000
  for (let i = 0; i < bytes.length; i += batchSize) {
    binary += String.fromCharCode(...bytes.subarray(i, i + batchSize))
  }
  return btoa(binary)
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

function getStepFunWsUrl(): string {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws/asr/stepfun`
}

function resetStreamingState() {
  if (streamSocket) {
    streamSocket.close()
    streamSocket = null
  }
  streamReady = false
  streamMessage.value = ''
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
      <strong v-if="uploading">离线识别中...</strong>
      <strong v-else-if="streamStatus === 'connecting'">{{ streamMessage || '连接实时识别...' }}</strong>
      <strong v-else-if="streamStatus === 'speaking'">{{ streamMessage || '正在转写...' }}</strong>
      <strong v-else-if="recording">{{ streamMessage || '正在聆听...' }}</strong>
      <strong v-else>{{ mode === 'stream' ? '实时语音输入' : 'Vosk 离线识别' }}</strong>
      <span>{{ mode === 'stream' ? '音频会以 16k PCM 分片发送到后端 StepFun 代理。' : '录音会转换为标准 WAV 后上传识别。' }}</span>
    </div>

    <div v-if="recording" class="level-track">
      <div class="level-fill" :style="{ width: `${Math.round(audioLevel * 100)}%` }"></div>
    </div>
  </section>
</template>
