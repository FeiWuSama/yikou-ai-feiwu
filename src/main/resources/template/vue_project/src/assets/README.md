# 静态资源目录

此目录用于存放项目的静态资源文件，如：

- 图片文件 (.jpg, .png, .gif, .svg)
- 字体文件 (.woff, .woff2, .ttf, .eot)
- 音频文件 (.mp3, .wav)
- 视频文件 (.mp4, .webm)

## 示例图片

您可以将图片文件放在此目录下，并在组件中使用：

```vue
<template>
  <img :src="require('@/assets/logo.png')" alt="Logo" />
</template>

<script setup>
// 或者使用 import
import logo from '@/assets/logo.png'
</script>
```

## 目录结构建议

```
assets/
├── images/          # 图片文件
│   ├── logo.png
│   └── background.jpg
├── fonts/           # 字体文件
│   └── custom-font.woff2
└── icons/           # 图标文件
    └── menu.svg
```