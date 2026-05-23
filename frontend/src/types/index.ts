export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface AudioUploadVO {
  recordId: number
  audioName: string
  audioSize: number
  audioFormat: string
  status: string
}

export interface AsrResponse {
  recordId: number
  originalText: string
  engineType: string
  duration: number
  confidence: number
}

export interface EngineOption {
  label: string
  value: string
  mode: 'stream' | 'upload'
}
