import axios from 'axios'
import type { ApiResult, AudioUploadVO } from '../types'

const http = axios.create({
  baseURL: '/api',
  timeout: 60000,
})

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResult<unknown>
    if (body.code !== 0) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return response
  },
  (error) => Promise.reject(error),
)

export async function uploadAudio(audio: Blob): Promise<ApiResult<AudioUploadVO>> {
  const form = new FormData()
  form.append('audio', audio, getAudioFileName(audio))
  const response = await http.post<ApiResult<AudioUploadVO>>('/audio/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return response.data
}

function getAudioFileName(audio: Blob): string {
  if (audio.type.includes('wav')) return 'recording.wav'
  if (audio.type.includes('ogg')) return 'recording.ogg'
  if (audio.type.includes('mpeg') || audio.type.includes('mp3')) return 'recording.mp3'
  return 'recording.webm'
}

export default http
