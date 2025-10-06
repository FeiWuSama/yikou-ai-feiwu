<template>
  <div class="global-header">
    <div class="header-left">
      <div class="logo-container">
        <img
          alt="Logo"
          class="logo"
          src="@/assets/logo.png"
        />
        <span class="title">易扣</span>
      </div>

      <a-menu
        v-model:selectedKeys="selectedKeys"
        mode="horizontal"
        class="nav-menu"
      >
        <a-menu-item key="home">
          <router-link to="/">首页</router-link>
        </a-menu-item>
<!--        <a-menu-item key="about">-->
<!--          <router-link to="/about">关于</router-link>-->
<!--        </a-menu-item>-->
      </a-menu>
    </div>
    <div class="header-right">
      <!-- 登录按钮，后续替换为用户头像和昵称 -->
      <a-button type="primary">登录</a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

const router = useRouter()
const selectedKeys = ref<string[]>([])

router.afterEach((to,from,next) => {
  selectedKeys.value = [to.name as string]
})
</script>

<style scoped>
.global-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 64px;
}

.header-left {
  display: flex;
  align-items: center;
  flex: 1;
}

.logo-container {
  display: flex;
  align-items: center;
  margin-right: 24px;
}

.logo {
  height: 32px;
  width: 32px;
  margin-right: 12px;
}

.title {
  font-weight: 600;
  font-size: 18px;
  color: #000;
}

.nav-menu {
  border: none;
  background: transparent;
  flex: 1;
}

.header-right {
  display: flex;
  align-items: center;
}
</style>
