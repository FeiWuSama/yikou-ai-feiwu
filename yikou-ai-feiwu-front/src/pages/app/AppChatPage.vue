<template>
  <div id="appChatPage">
    <!-- 顶部栏 -->
    <a-layout-header class="header">
      <div class="header-content">
        <div class="app-name">{{ appInfo.appName || '应用对话' }}</div>
        <a-button type="primary" @click="doDeploy" :loading="deployLoading">部署</a-button>
      </div>
    </a-layout-header>
    <a-layout class="main-layout">
      <!-- 左侧对话区域 -->
      <a-layout-content class="chat-container">
        <!-- 消息区域 -->
        <div
          class="messages-container"
          ref="messagesContainerRef"
          :style="{ height: messagesContainerHeight + 'px' }"
        >
          <div
            v-for="msg in messages"
            :key="msg.id"
            :class="['message', msg.role]"
            :data-id="msg.id"
          >
            <div class="message-content">
              <div class="avatar">
                <a-avatar
                  v-if="msg.role === 'user'"
                  :src="loginUserStore.loginUser.userAvatar"
                  :alt="loginUserStore.loginUser.userName"
                />
                <a-avatar
                  v-else
                  src="/src/assets/logo.png"
                  alt="AI助手"
                />
              </div>
              <div class="content">
                <MarkdownRenderer v-if="msg.role === 'ai' && msg.content" :content="msg.content" />
                <div v-else-if="msg.role === 'user'" class="user-content">{{ msg.content }}</div>
                <div v-else class="ai-content">AI在生成代码中...</div>
              </div>
            </div>
          </div>
        </div>
        <!-- 输入框 -->
        <div class="input-container">
          <a-tooltip :title="isInputDisabled ? '无法在别人的作品下对话哦~' : ''" placement="top">
            <a-input
              v-model:value="inputMessage"
              placeholder="请输入消息..."
              @pressEnter="sendMessage"
              :disabled="sending || isInputDisabled"
            />
          </a-tooltip>
          <a-button
            type="primary"
            @click="sendMessage"
            :loading="sending"
            :disabled="!inputMessage.trim() || isInputDisabled"
          >
            发送
          </a-button>
        </div>
      </a-layout-content>
      <!-- 右侧网页展示区域 -->
      <a-layout-sider width="60%" class="preview-container">
        <div v-if="streamLoading" class="loading-container">
          <a-spin size="large" />
          <p>AI正在生成中...</p>
        </div>
        <iframe v-else-if="deployUrl" :src="deployUrl" class="preview-frame" frameborder="0"></iframe>
        <div v-else class="empty-preview">
          <p>暂无预览内容</p>
        </div>
      </a-layout-sider>
    </a-layout>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, nextTick, computed } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute } from 'vue-router'
import { getAppVoById, chatToGenCode, deployApp } from '@/api/appController.ts'
import { EventSourcePolyfill } from 'event-source-polyfill'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'

const loginUserStore = useLoginUserStore()

const route = useRoute()

// 应用信息
const appInfo = ref<API.AppVO>({})
const appId = ref<string>()

// 消息相关
const messages = ref<Array<{ id: number; role: string; content: string }>>([])
const inputMessage = ref('')
const sending = ref(false)
const messagesContainerRef = ref<HTMLDivElement | null>(null)

// 消息容器高度相关
const messagesContainerHeight = ref(500)

// 部署相关
const deployLoading = ref(false)
const deployUrl = ref('')

// 流式响应相关
const streamLoading = ref(false)

// 权限相关
// 判断是否为自己的应用
const isMyApp = computed(() => {
  return appInfo.value.userId === loginUserStore.loginUser.id
})

// 输入框禁用状态
const isInputDisabled = computed(() => {
  // 如果应用信息还未加载，不禁用
  if (!appInfo.value.id) {
    return false
  }
  // 如果是自己的应用，不禁用
  if (isMyApp.value) {
    return false
  }
  // 如果不是自己的应用，禁用输入框
  return true
})

// 获取应用信息
const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('应用ID不存在')
    return
  }

  appId.value = id

  const res = await getAppVoById({ id: id })
  if (res.data.code === 0 && res.data.data) {
    appInfo.value = res.data.data
    // 检查是否有view参数，如果没有则自动发送初始提示词
    const viewParam = route.query.view
    if (!viewParam && appInfo.value.initPrompt) {
      await sendInitialMessage(appInfo.value.initPrompt)
    }
  } else {
    message.error('获取应用信息失败，' + res.data.message)
  }
}

// 发送初始消息
const sendInitialMessage = async (prompt: string) => {
  // 添加用户消息
  const userMsg = {
    id: Date.now(),
    role: 'user',
    content: prompt,
  }
  messages.value.push(userMsg)

  // 添加AI消息占位符
  const aiMsg = {
    id: Date.now() + 1,
    role: 'ai',
    content: '',
  }
  messages.value.push(aiMsg)

  // 滚动到底部
  await nextTick()
  scrollToBottom()

  // 发送请求
  await sendSSEMessage(prompt, aiMsg)
}

// 发送消息
const sendMessage = async () => {
  if (!inputMessage.value.trim() || sending.value) {
    return
  }

  const userMsgContent = inputMessage.value
  inputMessage.value = ''

  // 添加用户消息
  const userMsg = {
    id: Date.now(),
    role: 'user',
    content: userMsgContent,
  }
  messages.value.push(userMsg)

  // 添加AI消息占位符
  const aiMsg = {
    id: Date.now() + 1,
    role: 'ai',
    content: '',
  }
  messages.value.push(aiMsg)

  // 滚动到底部
  await nextTick()
  scrollToBottom()

  // 发送请求
  await sendSSEMessage(userMsgContent, aiMsg)
}

// 发送SSE消息
const sendSSEMessage = async (content: string, aiMsg: any) => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }

  sending.value = true
  streamLoading.value = true

  try {
    // 使用EventSource接收流式响应
    const eventSource = new EventSourcePolyfill(
      `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8123/api'}/app/chat/gen/code?appId=${appId.value}&message=${encodeURIComponent(content)}`,
      {
        withCredentials: true,
      },
    )

    eventSource.onmessage = (event) => {
      // 处理数据
      let processedData = ''

      try {
        // 解析JSON数据
        const jsonData = JSON.parse(event.data)
        // 提取d字段的内容
        processedData = jsonData.d || ''
      } catch (error) {
        // 如果JSON解析失败，直接使用原始数据
        console.warn('JSON解析失败，使用原始数据:', error)
        processedData = event.data
      }

      // 更新AI消息内容
      aiMsg.content += processedData

      // 强制更新DOM并滚动到底部
      nextTick().then(() => {
        scrollToBottom()
      })
    }

    // 监听特定事件类型
    eventSource.addEventListener('done', (event) => {
      // 流结束
      eventSource.close()
      sending.value = false
      streamLoading.value = false
      // 显示部署预览
      showPreview()
    })

    eventSource.onerror = (error) => {
      console.error('SSE error:', error)
      eventSource.close()
      sending.value = false
      streamLoading.value = false
      message.error('消息发送失败')
    }
  } catch (error) {
    sending.value = false
    streamLoading.value = false
    message.error('消息发送失败，请重试')
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainerRef.value) {
    messagesContainerRef.value.scrollTop = messagesContainerRef.value.scrollHeight
  }
}

// 显示预览
const showPreview = () => {
  if (appInfo.value.codeGenType && appInfo.value.id) {
    deployUrl.value = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8123/api'}/static/${appInfo.value.codeGenType}_${appInfo.value.id}/`
  }
}

// 部署应用
const doDeploy = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }

  deployLoading.value = true
  try {
    const res = await deployApp({ appId: appId.value })
    if (res.data.code === 0 && res.data.data) {
      message.success('部署成功')
      deployUrl.value = res.data.data
    } else {
      message.error('部署失败，' + res.data.message)
    }
  } catch (error) {
    message.error('部署失败，请重试')
  } finally {
    deployLoading.value = false
  }
}

// 页面加载时获取应用信息和用户信息
onMounted(async () => {
  // 获取用户信息
  await loginUserStore.fetchLoginUser()
  // 获取应用信息
  await fetchAppInfo()
})
</script>

<style scoped>
#appChatPage {
  height: 85vh;
  display: flex;
  flex-direction: column;
  margin: 20px auto;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.header {
  background: #fff;
  padding: 0 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 1;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 64px;
}

.app-name {
  font-size: 18px;
  font-weight: bold;
}

.main-layout {
  flex: 1;
}

.chat-container {
  display: flex;
  flex-direction: column;
  height: calc(90vh - 66px - 40px);
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f5f5f5;
  border-radius: 8px;
}

.message {
  margin-bottom: 20px;
}

.message-content {
  display: flex;
  max-width: 100%;
}

.message.user .message-content {
  margin-left: auto;
  flex-direction: row-reverse;
  justify-content: flex-end;
}

.avatar {
  margin-right: 10px;
}

.message.user .avatar {
  margin-right: 0;
  margin-left: 10px;
}

.content {
  background: #fff;
  padding: 10px 15px;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  max-width: 80%;
  word-wrap: break-word;
}

.message.user .content {
  background: #1890ff;
  color: #fff;
  margin-left: auto;
}

/* AI消息中的代码块样式 */
.ai-content {
  line-height: 1.6;
  width: 100%;
}

.ai-content pre {
  background: #f8f8f8;
  border: 1px solid #e1e1e1;
  border-radius: 6px;
  padding: 16px;
  margin: 10px 0;
  overflow-x: auto;
  font-family: 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.4;
}

.ai-content code {
  background: #f5f5f5;
  padding: 2px 4px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-size: 13px;
}

.ai-content pre code {
  background: none;
  padding: 0;
  border-radius: 0;
}

/* 代码高亮样式增强 */
.ai-content .hljs {
  background: #f8f8f8;
  padding: 0;
}

.ai-content .hljs-keyword {
  color: #d73a49;
  font-weight: bold;
}

.ai-content .hljs-string {
  color: #032f62;
}

.ai-content .hljs-comment {
  color: #6a737d;
  font-style: italic;
}

.ai-content .hljs-function {
  color: #6f42c1;
}

.ai-content .hljs-tag {
  color: #22863a;
}

.ai-content .hljs-attr {
  color: #6f42c1;
}

.ai-content .hljs-title {
  color: #6f42c1;
}

.input-container {
  display: flex;
  padding: 20px;
  background: #fff;
  border-top: 1px solid #e8e8e8;
}

.input-container .ant-input {
  flex: 1;
  margin-right: 10px;
}

.preview-container {
  background: #fff;
  border-left: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 0 8px 8px 0;
}

:deep(.ant-layout-sider-children){
  display: flex;
  justify-content: center;
  align-items: center;
  width: 60vw;
}

.preview-frame {
  width: 100%;
  height: 100%;
}

.loading-container {
  text-align: center;
  padding: 20px;
}

.empty-preview {
  text-align: center;
  color: #999;
}
</style>
