<template>
  <a-card hoverable class="app-card" :key="appData.id">
    <template #cover>
      <div class="cover-container" @mouseenter="showOverlay = true" @mouseleave="showOverlay = false">
        <img :src="appData.cover || defaultCover" alt="应用封面" class="app-cover" />
        <!-- 覆盖层 -->
        <div v-if="showOverlay" class="cover-overlay">
          <div class="action-buttons-container">
            <!-- 精选应用：只显示预览 -->
            <div v-if="isGoodApp" class="capsule-button preview-only" @click="handleAction('preview', appData)">
              <span class="action-icon">👁️</span>
              <span class="action-text">预览</span>
            </div>
            <!-- 我的应用：显示三个操作 -->
            <div v-else class="capsule-buttons-horizontal">
              <div class="capsule-button continue-action" @click="handleAction('continue', appData)">
                <span class="action-icon">✏️</span>
                <span class="action-text">继续创作</span>
              </div>
              <div class="capsule-button edit-action" @click="handleAction('edit', appData)">
                <span class="action-icon">⚙️</span>
                <span class="action-text">编辑应用</span>
              </div>
              <div class="capsule-button delete-action" @click="handleAction('delete', appData)">
                <span class="action-icon">🗑️</span>
                <span class="action-text">删除应用</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
    <div class="app-info">
      <span class="app-name">{{ appData.appName }}</span>
      <div class="user-info" v-if="showUserInfo">
        <a-avatar :src="appData.user?.userAvatar" size="small" />
        <span class="app-user-name">{{ appData.user?.userName }}</span>
      </div>
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { defineProps, defineEmits, ref, computed } from 'vue'
import defaultCover from '@/assets/logo.png'

interface AppCardProps {
  appData: API.AppVO
  actions?: Array<{
    key: string
    label: string
  }>
  showUserInfo?: boolean
  isGoodApp?: boolean // 是否为精选应用
}

const props = withDefaults(defineProps<AppCardProps>(), {
  actions: () => [],
  showUserInfo: false,
  isGoodApp: false
})

const emit = defineEmits<{
  action: [actionKey: string, appData: API.AppVO]
}>()

const showOverlay = ref(false)

const handleAction = (actionKey: string, appData: API.AppVO) => {
  emit('action', actionKey, appData)
}
</script>

<style scoped>
.app-card {
  width: 300px;
  margin: 10px;
  position: relative;
}

.cover-container {
  position: relative;
  overflow: hidden;
}

.app-cover {
  height: 150px;
  object-fit: cover;
  width: 100%;
  transition: all 0.3s ease;
}

.cover-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  opacity: 0;
  animation: fadeIn 0.3s ease forwards;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.action-buttons-container {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10px;
  gap: 10px;
}

.capsule-buttons-horizontal {
  display: flex;
  align-items: center;
  gap: 8px;
}

.capsule-button {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 0 20px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 15px;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 70px;
  text-align: center;
  color: #333;
  border: 1px solid rgba(0, 0, 0, 0.1);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

.capsule-button:hover {
  background: rgba(255, 255, 255, 1);
  transform: scale(1.05);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.25);
}

.preview-only {
  background: rgba(255, 255, 255, 0.95);
  color: #333;
  min-width: 90px;
  padding: 8px 20px;
}

.preview-only:hover {
  background: rgba(255, 255, 255, 1);
}

.continue-action {
  background: rgba(255, 255, 255, 0.95);
  color: #333;
}

.continue-action:hover {
  background: rgba(255, 255, 255, 1);
}

.edit-action {
  background: rgba(255, 255, 255, 0.95);
  color: #333;
}

.edit-action:hover {
  background: rgba(255, 255, 255, 1);
}

.delete-action {
  background: rgba(255, 255, 255, 0.95);
  color: #333;
}

.delete-action:hover {
  background: rgba(255, 255, 255, 1);
}

.action-icon {
  font-size: 16px;
  margin-bottom: 3px;
}

.action-text {
  font-size: 11px;
  font-weight: 500;
}

.app-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 8px;
}

.app-name {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin-bottom: 3px;
}

.user-info {
  display: flex;
  align-items: center;
  margin-top: 3px;
}

.app-user-name {
  margin-left: 8px;
  font-size: 12px;
  color: #666;
}

:deep(.ant-card-body){
  padding: 5px;
}

@media (max-width: 768px) {
  .app-card {
    width: 100%;
  }

  .capsule-buttons-horizontal {
    gap: 4px;
  }

  .capsule-button {
    min-width: 60px;
    padding: 0 20px;
  }

  .action-icon {
    font-size: 14px;
  }

  .action-text {
    font-size: 10px;
  }

  .action-buttons-container {
    margin-bottom: 15px;
  }

  .app-info {
    padding: 6px;
  }

  .app-name {
    margin-bottom: 4px;
  }

  .user-info {
    margin-top: 4px;
  }
}

@media (max-width: 480px) {
  .capsule-buttons-horizontal {
    flex-direction: column;
    gap: 4px;
  }

  .capsule-button {
    min-width: 100px;
    padding: 0 20px;
  }

  .action-buttons-container {
    margin-bottom: 25px;
  }

  .app-info {
    padding: 6px;
  }

  .app-name {
    margin-bottom: 4px;
  }

  .user-info {
    margin-top: 4px;
  }
}
</style>
