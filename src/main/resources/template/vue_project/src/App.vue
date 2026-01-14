<template>
  <div id="app">
    <header class="app-header">
      <h1>{{ title }}</h1>
    </header>
    <main class="app-main">
      <router-view />
    </main>
    <footer class="app-footer">
      <p>© {{ new Date().getFullYear() }} Vue Project Template</p>
    </footer>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const title = ref('Vue Project Template')
</script>

<style scoped>
.app-header {
  background-color: #409eff;
  color: white;
  padding: 1rem;
  text-align: center;
}

.app-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
  min-height: calc(100vh - 140px);
}

.app-footer {
  text-align: center;
  padding: 1rem;
  background-color: #f5f5f5;
  border-top: 1px solid #e9e9e9;
}
</style>