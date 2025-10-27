# 易扣AI - AI应用生成平台

<p align="center">
  <img src="yikou-ai-feiwu-front/src/assets/logo.png" alt="易扣AI Logo" width="200">
</p>

<p align="center">
  <a href="https://github.com/FeiWuSama/yikou-ai-feiwu/stargazers">
    <img src="https://img.shields.io/github/stars/FeiWuSama/yikou-ai-feiwu" alt="GitHub Stars">
  </a>
  <a href="https://github.com/FeiWuSama/yikou-ai-feiwu/issues">
    <img src="https://img.shields.io/github/issues/FeiWuSama/yikou-ai-feiwu" alt="GitHub Issues">
  </a>
  <a href="https://github.com/FeiWuSama/yikou-ai-feiwu/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/FeiWuSama/yikou-ai-feiwu" alt="GitHub License">
  </a>
</p>

易扣AI是一个基于人工智能的零代码应用生成平台，用户只需描述需求，即可自动生成完整的Web应用程序。平台支持多种应用类型，包括静态网站、多页面应用和Vue项目，并提供可视化编辑功能。

## 🌟 特性

- **零代码开发**：通过自然语言描述需求，自动生成完整应用
- **多应用类型支持**：支持HTML单页应用、多文件应用和Vue项目
- **可视化编辑**：在预览页面中直接选择元素进行修改
- **实时预览**：边生成边预览，实时查看应用效果
- **一键部署**：生成的应用可一键部署到服务器
- **代码下载**：可下载生成的完整源代码进行二次开发

## 🏗️ 技术架构

### 前端技术栈
- **Vue 3** + **TypeScript**：现代化前端框架
- **Vite**：快速构建工具
- **Ant Design Vue**：UI组件库
- **Pinia**：状态管理
- **Vue Router**：路由管理

### 后端技术栈
- **Spring Boot 3.5**：Java后端框架
- **MySQL**：关系型数据库
- **Redis**：缓存和会话存储
- **LangChain4j**：Java语言的AI集成框架
- **MyBatis-Flex**：ORM框架

### AI能力
- **多种AI模型支持**：支持OpenAI、阿里云DashScope等模型
- **智能代码生成**：基于提示词工程的代码生成
- **工具调用**：支持文件读写、目录操作等工具调用
- **质量检查**：生成代码的质量检查和优化

## 🚀 快速开始

### 环境要求
- **Node.js** >= 20.19.0
- **Java** >= 21
- **MySQL** >= 8.0
- **Redis** >= 6.0
- **Maven** >= 3.8

### 后端部署

1. 创建数据库：
```sql
create database if not exists yikou_ai;
```

2. 修改配置文件 `src/main/resources/application.yml` 中的数据库和Redis连接信息

3. 构建并运行：
```bash
mvn clean install
mvn spring-boot:run
```

### 前端部署

1. 进入前端目录：
```bash
cd yikou-ai-feiwu-front
```

2. 安装依赖：
```bash
npm install
```

3. 启动开发服务器：
```bash
npm run dev
```

4. 构建生产版本：
```bash
npm run build
```

## 📁 项目结构

```
yikou-ai-feiwu/
├── src/main/java/              # 后端Java源码
│   ├── com/feiwu/yikouai/ai/   # AI相关功能
│   ├── com/feiwu/yikouai/controller/  # 控制器
│   ├── com/feiwu/yikouai/service/     # 业务逻辑
│   └── com/feiwu/yikouai/core/        # 核心功能
├── src/main/resources/         # 后端资源配置
│   ├── prompt/                 # AI提示词模板
│   └── application.yml         # 配置文件
├── yikou-ai-feiwu-front/       # 前端项目
│   ├── src/
│   │   ├── pages/              # 页面组件
│   │   ├── components/         # 公共组件
│   │   ├── api/                # API接口
│   │   └── utils/              # 工具函数
│   └── package.json            # 前端依赖
└── sql/                        # 数据库脚本
```

## 🧠 核心功能

### 应用生成流程
1. 用户输入应用需求描述
2. 系统根据需求选择合适的AI模型和生成策略
3. AI生成代码并通过工具调用写入文件系统
4. 实时展示生成进度和预览效果
5. 生成完成后可进行可视化编辑和部署

### 可视化编辑
- 在预览页面中直接点击元素进行选择
- 通过自然语言描述修改需求
- AI理解需求并自动修改对应代码
- 实时预览修改效果

### 部署功能
- 一键部署生成的应用
- 支持自定义部署域名
- 自动生成部署链接

## 🛠️ 开发指南

### 数据库设计
项目包含三个核心数据表：
- `user`：用户表，存储用户信息
- `app`：应用表，存储生成的应用信息
- `chat_history`：对话历史表，存储用户与AI的交互记录

### API接口
后端提供RESTful API接口，主要包含：
- 用户管理：注册、登录、权限验证
- 应用管理：创建、查询、更新、删除应用
- 对话管理：获取对话历史、流式对话
- 静态资源：提供生成应用的预览和下载

### AI集成
通过LangChain4j框架集成多种AI模型：
- 提示词模板管理
- 工具调用机制
- 流式响应处理
- 质量检查和优化

## 📊 监控与运维

- 集成Prometheus监控指标
- 提供健康检查接口
- AI模型调用监控
- 性能指标收集

## 🤝 贡献

欢迎提交Issue和Pull Request来改进项目。

## 📄 许可证

本项目采用MIT许可证，详情请见[LICENSE](LICENSE)文件。

## 🙏 致谢

- [Vue.js](https://vuejs.org/)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [LangChain4j](https://github.com/langchain4j/langchain4j)
- [Ant Design Vue](https://www.antdv.com/)
