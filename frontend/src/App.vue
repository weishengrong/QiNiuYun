<script setup lang="ts">
const features = [
  '前后端基础工程已初始化',
  '后端提供健康检查接口',
  '后续 PR 将逐步接入录音、离线识别与实时识别',
]
</script>

<template>
  <main class="app-shell">
    <section class="hero">
      <div class="brand-mark" aria-hidden="true"></div>
      <div>
        <p class="eyebrow">QiNiuYun Voice Input</p>
        <h1>语音输入法</h1>
        <p class="summary">一个面向文本输入效率的 Web 语音输入工具，将通过小粒度 PR 逐步完善。</p>
      </div>
    </section>

    <section class="panel">
      <h2>当前 PR 范围</h2>
      <ul>
        <li v-for="feature in features" :key="feature">{{ feature }}</li>
      </ul>
    </section>
  </main>
</template>
