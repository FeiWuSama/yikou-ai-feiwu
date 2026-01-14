<template>
  <button class="example-button" :class="{ 'example-button-primary': primary }" @click="handleClick">
    <slot></slot>
  </button>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue'

// 定义组件属性
const props = defineProps({
  // 是否为主要按钮样式
  primary: {
    type: Boolean,
    default: false
  }
})

// 定义组件事件
const emit = defineEmits(['click'])

// 处理点击事件
const handleClick = () => {
  emit('click')
}
</script>

<style scoped>
.example-button {
  padding: 0.5rem 1rem;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background-color: #fff;
  cursor: pointer;
  font-size: 1rem;
  transition: all 0.3s ease;
}

.example-button:hover {
  border-color: #409eff;
  color: #409eff;
}

.example-button-primary {
  background-color: #409eff;
  color: white;
  border-color: #409eff;
}

.example-button-primary:hover {
  background-color: #66b1ff;
  border-color: #66b1ff;
  color: white;
}
</style>