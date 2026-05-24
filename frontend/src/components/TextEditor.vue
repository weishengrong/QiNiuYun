<script setup lang="ts">
const props = defineProps<{
  text: string
}>()

const emit = defineEmits<{
  updateText: [text: string]
}>()

async function copyText() {
  if (!props.text) return
  await navigator.clipboard.writeText(props.text)
}

function clearText() {
  emit('updateText', '')
}
</script>

<template>
  <section class="editor-panel">
    <textarea
      :value="text"
      placeholder="开始说话，识别结果会显示在这里..."
      @input="emit('updateText', ($event.target as HTMLTextAreaElement).value)"
    ></textarea>
    <div class="editor-actions">
      <button :disabled="!text" @click="copyText">复制</button>
      <button :disabled="!text" @click="clearText">清空</button>
    </div>
  </section>
</template>
