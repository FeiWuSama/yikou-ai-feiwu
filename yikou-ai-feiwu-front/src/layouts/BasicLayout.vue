<template>
  <a-layout class="layout-container">
    <!-- 导航栏 -->
    <a-layout-header class="header">
      <GlobalHeader />
    </a-layout-header>

    <!-- 内容区域 -->
    <a-layout-content class="content">
      <router-view />
    </a-layout-content>

    <!-- 底部 -->
    <a-layout-footer class="footer">
      <GlobalFooter />
    </a-layout-footer>
  </a-layout>
</template>

<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
}

.header {
  position: sticky;
  top: 0;
  z-index: 100;
  padding: 0;
  background: #fff;
  box-shadow: 0 2px 8px #f0f1f2;
}

.content {
  flex: 1;
  padding: 20px 0;
  margin: 0 auto;
  max-width: 1200px;
  width: 100%;
}

.footer {
  text-align: center;
  background: #f0f2f5;
  padding: 24px 0;
}
</style>
