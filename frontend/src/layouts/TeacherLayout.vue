<template>
  <BaseLayout :menu-groups="menuGroups" />
</template>

<script setup>
import BaseLayout from './BaseLayout.vue'

const menuGroups = [
  {
    title: '审核管理',
    items: [{ label: '待审核列表', to: '/teacher/review/tasks' }]
  },
  {
    title: '公告通知',
    items: [{ label: '公告管理', to: '/teacher/notices' }]
  },
  {
    title: '反馈管理',
    items: [
      { label: '我要反馈', to: '/teacher/feedback/create' },
      { label: '反馈处理', to: '/teacher/feedback/handle' }
    ]
  },
  {
    title: '排名公示',
    items: [{ label: '综合排名', to: '/teacher/ranking' }]
  }
]
</script>

