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
