<template>
  <div class="home">
    <div class="hero">
      <h2>欢迎使用 Vue 项目模板</h2>
      <p>这是一个基于 Vue 3 + Vite 构建的现代化前端项目模板</p>
      <div class="hero-actions">
        <example-button @click="handlePrimaryClick" primary>主要按钮</example-button>
        <example-button @click="handleSecondaryClick">次要按钮</example-button>
      </div>
    </div>
    
    <div class="features">
      <div class="feature-item">
        <h3>📦 现代化构建</h3>
        <p>基于 Vite 构建，提供极速的开发体验和优化的生产构建</p>
      </div>
      <div class="feature-item">
        <h3>🚀 Vue 3</h3>
        <p>使用 Vue 3 的组合式 API，提供更灵活的组件逻辑组织方式</p>
      </div>
      <div class="feature-item">
        <h3>🧭 Vue Router</h3>
        <p>集成 Vue Router 4，提供强大的路由管理功能</p>
      </div>
      <div class="feature-item">
        <h3>🧩 组件化开发</h3>
        <p>支持组件化开发，提高代码复用性和可维护性</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import ExampleButton from '../components/ExampleButton.vue'

// 按钮点击事件处理
const handlePrimaryClick = () => {
  alert('主要按钮被点击了！')
}

const handleSecondaryClick = () => {
  alert('次要按钮被点击了！')
}
</script>

<style scoped>
.home {
  padding: 2rem 0;
}

.hero {
  text-align: center;
  margin-bottom: 3rem;
}

.hero h2 {
    font-size: 2.5rem;
    margin-bottom: 1rem;
    color: #303133;
  }

  .hero p {
    font-size: 1.2rem;
    color: #606266;
    margin-bottom: 2rem;
  }

  .hero-actions {
    display: flex;
    gap: 1rem;
    margin-top: 2rem;
  }

.features {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 2rem;
  margin-top: 3rem;
}

.feature-item {
  background-color: #fafafa;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.feature-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 20px 0 rgba(0, 0, 0, 0.15);
}

.feature-item h3 {
  font-size: 1.5rem;
  margin-bottom: 1rem;
  color: #303133;
}

.feature-item p {
  color: #606266;
  line-height: 1.6;
}
</style>