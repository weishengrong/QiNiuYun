<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { deleteRecord, fetchRecords, updateRecordText } from '../api'
import type { RecordPageData, RecordVO } from '../types'

const emit = defineEmits<{
  reuse: [text: string]
  error: [message: string]
}>()

const pageData = ref<RecordPageData>({
  total: 0,
  page: 1,
  size: 20,
  records: [],
})
const loading = ref(false)
const editingId = ref<number | null>(null)
const editText = ref('')

const totalPages = computed(() => Math.max(1, Math.ceil(pageData.value.total / pageData.value.size)))

async function loadRecords() {
  loading.value = true
  try {
    const response = await fetchRecords(pageData.value.page, pageData.value.size)
    pageData.value = response.data
  } catch (error) {
    emit('error', error instanceof Error ? error.message : '历史记录加载失败')
  } finally {
    loading.value = false
  }
}

function getDisplayText(record: RecordVO): string {
  return record.editedText || record.originalText || ''
}

function startEdit(record: RecordVO) {
  editingId.value = record.id
  editText.value = getDisplayText(record)
}

function cancelEdit() {
  editingId.value = null
  editText.value = ''
}

async function saveEdit(id: number) {
  if (!editText.value.trim()) return
  try {
    await updateRecordText(id, editText.value)
    cancelEdit()
    await loadRecords()
  } catch (error) {
    emit('error', error instanceof Error ? error.message : '保存失败')
  }
}

async function removeRecord(id: number) {
  try {
    await deleteRecord(id)
    await loadRecords()
  } catch (error) {
    emit('error', error instanceof Error ? error.message : '删除失败')
  }
}

async function goPage(page: number) {
  if (page < 1 || page > totalPages.value) return
  pageData.value.page = page
  await loadRecords()
}

function getStatusText(status: number): string {
  if (status === 1) return '成功'
  if (status === 2) return '失败'
  return '处理中'
}

onMounted(loadRecords)
</script>

<template>
  <section class="history-panel">
    <header class="history-header">
      <h2>历史记录</h2>
      <button @click="loadRecords">刷新</button>
    </header>

    <div v-if="loading" class="empty-state">加载中...</div>
    <div v-else-if="pageData.records.length === 0" class="empty-state">暂无历史记录</div>

    <div v-else class="record-list">
      <article v-for="record in pageData.records" :key="record.id" class="record-item">
        <div class="record-meta">
          <span>{{ record.createdAt }}</span>
          <span>{{ record.engineType || 'unknown' }}</span>
          <span>{{ getStatusText(record.status) }}</span>
        </div>

        <div v-if="editingId === record.id" class="record-edit">
          <textarea v-model="editText" rows="3"></textarea>
          <div class="record-actions">
            <button @click="saveEdit(record.id)">保存</button>
            <button @click="cancelEdit">取消</button>
          </div>
        </div>

        <p v-else class="record-text">{{ getDisplayText(record) || '暂无识别文本' }}</p>

        <div class="record-actions">
          <button :disabled="!getDisplayText(record)" @click="emit('reuse', getDisplayText(record))">复用</button>
          <button @click="startEdit(record)">编辑</button>
          <button @click="removeRecord(record.id)">删除</button>
        </div>
      </article>
    </div>

    <footer v-if="pageData.total > pageData.size" class="pagination">
      <button :disabled="pageData.page <= 1" @click="goPage(pageData.page - 1)">上一页</button>
      <span>{{ pageData.page }} / {{ totalPages }}</span>
      <button :disabled="pageData.page >= totalPages" @click="goPage(pageData.page + 1)">下一页</button>
    </footer>
  </section>
</template>
