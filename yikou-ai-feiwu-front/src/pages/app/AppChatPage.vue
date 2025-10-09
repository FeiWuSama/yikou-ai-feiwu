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
                <div
                  v-if="msg.role === 'ai'"
                  class="ai-content"
                  v-html="marked.parse(msg.content)"
                ></div>
                <div v-else class="user-content">{{ msg.content }}</div>
              </div>
            </div>
          </div>
        </div>

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
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import { useLoginUserStore } from '@/stores/loginUser.ts'

const loginUserStore = useLoginUserStore()

const route = useRoute()

// 配置markdown解析器
marked.setOptions({
  highlight: function (code, lang) {
    try {
      // 自动检测语言类型
      let language = 'plaintext'

      // 根据文件扩展名或内容自动识别语言
      if (lang) {
        language = hljs.getLanguage(lang) ? lang : 'plaintext'
      } else {
        // 自动检测代码语言
        const detected = hljs.highlightAuto(code)
        language = detected.language || 'plaintext'
      }

      return hljs.highlight(code, { language }).value
    } catch (error) {
      console.warn('代码高亮失败:', error)
      return code
    }
  },
  breaks: true,
  gfm: true,
  langPrefix: 'hljs language-',
})

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

  appId.value = id

  const res = await getAppVoById({ id: id })
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

// HTML代码解析器（仿照后端实现）
const parseHtmlCode = (content: string): { htmlCode: string; description: string } => {
  // 正则表达式匹配HTML代码块（仿照后端的HTML_CODE_PATTERN）
  const HTML_CODE_PATTERN = /```html\s*\n([\s\S]*?)```/i

  const result = { htmlCode: '', description: '' }

  // 提取HTML代码
  const matcher = content.match(HTML_CODE_PATTERN)
  if (matcher && matcher[1]) {
    result.htmlCode = matcher[1].trim()
    // 提取描述部分（代码块之前的内容）
    const beforeCode = content.substring(0, content.indexOf('```html')).trim()
    if (beforeCode) {
      result.description = beforeCode
    }
  } else {
    // 如果没有找到代码块，将整个内容作为HTML（仿照后端逻辑）
    result.htmlCode = content.trim()
  }

  return result
}

// 智能检测代码块并格式化为markdown
const formatCodeBlocks = (content: string): string => {
  // 如果内容已经包含代码块标记，直接返回
  if (content.includes('```')) {
    return content
  }

  // 首先尝试使用HTML代码解析器（仿照后端逻辑）
  const parsedResult = parseHtmlCode(content)

  // 如果解析出了HTML代码，优先使用解析结果
  if (parsedResult.htmlCode && parsedResult.htmlCode !== content) {
    if (parsedResult.description) {
      return `${parsedResult.description}\n\n\`\`\`html\n${parsedResult.htmlCode}\n\`\`\``
    } else {
      return `\`\`\`html\n${parsedResult.htmlCode}\n\`\`\``
    }
  }

  // 检测完整的HTML文档结构（包含DOCTYPE和html标签）
  const htmlDocPattern = /<!DOCTYPE html>[\s\S]*?<\/html>/i
  const htmlMatch = content.match(htmlDocPattern)

  if (htmlMatch) {
    // 提取HTML代码部分
    const htmlCode = htmlMatch[0]
    // 提取说明文字部分（HTML代码之前和之后的内容）
    const beforeCode = content.substring(0, content.indexOf(htmlCode)).trim()
    const afterCode = content.substring(content.indexOf(htmlCode) + htmlCode.length).trim()

    // 重新组织内容：说明文字在前，代码块在后
    let description = ''
    if (beforeCode) {
      description += beforeCode
    }
    if (afterCode) {
      if (description) description += '\n\n' + afterCode
      else description = afterCode
    }

    if (description) {
      return `${description}\n\n\`\`\`html\n${htmlCode}\n\`\`\``
    } else {
      return `\`\`\`html\n${htmlCode}\n\`\`\``
    }
  }

  // 检测混合代码内容（HTML + CSS + JavaScript + 说明文字）
  const hasHTML = /<\/?[a-z][\s\S]*?>/i.test(content)
  const hasCSS = /\{[\s\S]*?\}/.test(content) || /\.\w+\s*\{/.test(content)
  const hasJS = /(function|const|let|var|document\.getElementById)/.test(content)

  // 如果包含多种代码类型，认为是完整的网页代码
  if ((hasHTML && hasCSS) || (hasHTML && hasJS) || (hasHTML && hasCSS && hasJS)) {
    // 提取说明文字部分（非代码内容）
    const lines = content.split('\n')
    const codeLines: string[] = []
    const textLines: string[] = []

    // 分离代码行和文本行（更智能的检测）
    let inCodeSection = false
    lines.forEach((line) => {
      const trimmedLine = line.trim()

      // 检测是否进入代码区域
      if (
        trimmedLine.includes('<!DOCTYPE') ||
        trimmedLine.includes('<html') ||
        trimmedLine.includes('<head>') ||
        trimmedLine.includes('<body>') ||
        trimmedLine.includes('<style>') ||
        trimmedLine.includes('<script>')
      ) {
        inCodeSection = true
      }

      // 检测是否退出代码区域
      if (trimmedLine.includes('</html>') || trimmedLine.includes('</body>')) {
        inCodeSection = false
      }

      if (
        inCodeSection ||
        (trimmedLine &&
          !trimmedLine.startsWith('//') &&
          !trimmedLine.startsWith('*') &&
          !trimmedLine.startsWith('这个') &&
          !trimmedLine.startsWith('页面') &&
          !trimmedLine.startsWith('响应式') &&
          !trimmedLine.startsWith('简洁') &&
          !trimmedLine.startsWith('包含') &&
          !trimmedLine.startsWith('特点：') &&
          !trimmedLine.startsWith('使用了') &&
          !trimmedLine.startsWith('同时') &&
          !trimmedLine.startsWith('JavaScript') &&
          !trimmedLine.startsWith('这是') &&
          !trimmedLine.startsWith('我将') &&
          !trimmedLine.startsWith('总代码') &&
          !trimmedLine.startsWith('页面包含') &&
          !trimmedLine.startsWith('点击登录') &&
          !trimmedLine.startsWith('基本的') &&
          !trimmedLine.startsWith('简洁美观') &&
          !trimmedLine.startsWith('响应式设计') &&
          !trimmedLine.startsWith('登录页面包含以下特点：') &&
          !trimmedLine.endsWith('。') &&
          !trimmedLine.endsWith('：') &&
          trimmedLine.length > 3)
      ) {
        codeLines.push(line)
      } else if (trimmedLine) {
        textLines.push(line)
      }
    })

    // 重新组织内容：文本在前，代码块在后
    const textContent = textLines.join('\n').trim()
    const codeContent = codeLines.join('\n').trim()

    if (textContent && codeContent) {
      return `${textContent}\n\n\`\`\`html\n${codeContent}\n\`\`\``
    } else if (codeContent) {
      return `\`\`\`html\n${codeContent}\n\`\`\``
    }
  }

  // 检测其他代码模式
  const codePatterns = [
    // HTML标签检测
    /<\/?[a-z][\s\S]*?>/i,
    // CSS样式块检测
    /<style>[\s\S]*?<\/style>/i,
    // JavaScript脚本块检测
    /<script>[\s\S]*?<\/script>/i,
    // 函数定义检测
    /(function|const|let|var)\s+\w+\s*=/,
    // 代码块结构检测
    /^\s*<[\s\S]*?>\s*$/m,
  ]

  // 计算代码内容的比例
  const codeLines = content.split('\n').filter((line) => {
    return codePatterns.some((pattern) => pattern.test(line))
  })

  const codeRatio = codeLines.length / content.split('\n').length

  // 如果代码比例超过30%，认为是代码内容
  if (codeRatio > 0.3) {
    // 自动检测语言类型
    let language = 'plaintext'
    if (content.includes('<!DOCTYPE') || content.includes('<html')) {
      language = 'html'
    } else if (content.includes('<style>') || (content.includes('{') && content.includes('}'))) {
      language = 'css'
    } else if (
      content.includes('<script>') ||
      content.includes('function') ||
      content.includes('const') ||
      content.includes('let')
    ) {
      language = 'javascript'
    }

    return `\`\`\`${language}\n${content}\n\`\`\``
  }

  return content
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

      // 智能格式化代码块（只在内容变化时检测）
      let formattedContent = aiMsg.content
      if (!aiMsg._hasCodeBlock) {
        formattedContent = formatCodeBlocks(aiMsg.content)
        // 如果检测到代码块，标记为已处理
        if (formattedContent.includes('```')) {
          aiMsg._hasCodeBlock = true
        }
      }

      // 实时更新显示（将markdown转换为HTML）
      const aiContentElement = document.querySelector(`.message[data-id="${aiMsg.id}"] .ai-content`)
      if (aiContentElement) {
        aiContentElement.innerHTML = marked.parse(formattedContent)
      }

      // 滚动到底部
      scrollToBottom()
    }

    // 监听特定事件类型
    eventSource.addEventListener('done', (event) => {
      // 流结束
      eventSource.close()
      sending.value = false
      // 显示部署预览
      showPreview()
    })

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
  // 测试代码格式化功能
  // testFormatCodeBlocks()
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
  flex-direction: row-reverse;
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
}

.message.user .content {
  background: #1890ff;
  color: #fff;
}

/* AI消息中的代码块样式 */
.ai-content {
  line-height: 1.6;
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
}

.preview-frame {
  width: 100%;
  height: 100%;
}
</style>
