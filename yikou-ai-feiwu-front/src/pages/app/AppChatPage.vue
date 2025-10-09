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
          <div v-for="msg in messages" :key="msg.id" :class="['message', msg.role]">
            <div class="message-content">
              <div class="avatar">
                <a-avatar :icon="msg.role === 'user' ? 'user' : 'robot'" />
              </div>
              <div class="content">
                <div v-if="msg.role === 'ai'" class="ai-content" v-html="msg.content"></div>
                <div v-else class="user-content">{{ msg.content }}</div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 可拖拽分隔条 -->
        <div 
          class="resize-handle" 
          @mousedown="startResize"
        ></div>
        
        <!-- 输入框 -->
        <div class="input-container">
          <a-input 
            v-model:value="inputMessage" 
            placeholder="请输入消息..." 
            @pressEnter="sendMessage"
            :disabled="sending"
          />
          <a-button 
            type="primary" 
            @click="sendMessage" 
            :loading="sending"
            :disabled="!inputMessage.trim()"
          >
            发送
          </a-button>
        </div>
      </a-layout-content>
      
      <!-- 右侧网页展示区域 -->
      <a-layout-sider width="50%" class="preview-container" v-if="deployUrl">
        <iframe :src="deployUrl" class="preview-frame" frameborder="0"></iframe>
      </a-layout-sider>
    </a-layout>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute } from 'vue-router'
import { getAppVoById, chatToGenCode, deployApp } from '@/api/appController.ts'
import { EventSourcePolyfill } from 'event-source-polyfill'

const route = useRoute()

// 应用信息
const appInfo = ref<API.AppVO>({})
const appId = ref<number>()

// 消息相关
const messages = ref<Array<{id: number, role: string, content: string}>>([])
const inputMessage = ref('')
const sending = ref(false)
const messagesContainerRef = ref<HTMLDivElement | null>(null)

// 消息容器高度相关
const messagesContainerHeight = ref(500)
let isResizing = false

// 部署相关
const deployLoading = ref(false)
const deployUrl = ref('')

// 获取应用信息
const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('应用ID不存在')
    return
  }
  
  appId.value = parseInt(id)
  
  const res = await getAppVoById({ id: appId.value })
  if (res.data.code === 0 && res.data.data) {
    appInfo.value = res.data.data
    // 自动发送初始提示词
    if (appInfo.value.initPrompt) {
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
    content: prompt
  }
  messages.value.push(userMsg)
  
  // 添加AI消息占位符
  const aiMsg = {
    id: Date.now() + 1,
    role: 'ai',
    content: ''
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
    content: userMsgContent
  }
  messages.value.push(userMsg)
  
  // 添加AI消息占位符
  const aiMsg = {
    id: Date.now() + 1,
    role: 'ai',
    content: ''
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
  
  try {
    // 使用EventSource接收流式响应
    const eventSource = new EventSourcePolyfill(
      `/api/app/chat/gen/code?appId=${appId.value}&message=${encodeURIComponent(content)}`,
      {
        withCredentials: true
      }
    )
    
    eventSource.onmessage = (event) => {
      if (event.data === '[DONE]') {
        // 流结束
        eventSource.close()
        sending.value = false
        // 显示部署预览
        showPreview()
        return
      }
      
      // 解码数据
      const decodedData = decodeURIComponent(escape(atob(event.data)))
      
      // 更新AI消息内容
      aiMsg.content += decodedData
      
      // 滚动到底部
      scrollToBottom()
    }
    
    eventSource.onerror = (error) => {
      console.error('SSE error:', error)
      eventSource.close()
      sending.value = false
      message.error('消息发送失败')
    }
  } catch (error) {
    sending.value = false
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
    deployUrl.value = `http://localhost:8123/api/static/${appInfo.value.codeGenType}_${appInfo.value.id}/`
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

// 页面加载时获取应用信息
onMounted(() => {
  fetchAppInfo()
})
</script>

<style scoped>
#appChatPage {
  height: 100vh;
  display: flex;
  flex-direction: column;
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
  height: calc(100vh - 64px);
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f5f5f5;
}

.message {
  margin-bottom: 20px;
}

.message-content {
  display: flex;
  max-width: 80%;
}

.message.user .message-content {
  margin-left: auto;
}

.avatar {
  margin-right: 10px;
}

.content {
  background: #fff;
  padding: 10px 15px;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}

.message.user .content {
  background: #1890ff;
  color: #fff;
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
}

.preview-frame {
  width: 100%;
  height: 100%;
}
</style>