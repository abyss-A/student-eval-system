<template>
  <BaseLayout :menu-groups="menuGroups" />
</template>

<script setup>
import BaseLayout from './BaseLayout.vue'

const menuGroups = [
  {
    title: '终审管理',
    items: [{ label: '待终审列表', to: '/admin/finalize/tasks' }]
  },
  {
    title: '公告通知',
    items: [{ label: '公告管理', to: '/admin/notices' }]
  },
  {
    title: '反馈管理',
    items: [
      { label: '我要反馈', to: '/admin/feedback/create' },
      { label: '反馈处理', to: '/admin/feedback/handle' }
    ]
  },
  {
    title: '排名公示',
    items: [{ label: '综合排名', to: '/admin/ranking' }]
  }
]
</script>

