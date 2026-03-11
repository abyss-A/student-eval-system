<template>
  <BaseLayout :menu-groups="menuGroups" />
</template>

<script setup>
import BaseLayout from './BaseLayout.vue'

const menuGroups = [
  {
    title: '审核管理',
    defaultOpen: true,
    items: [{ label: '待审核列表', to: '/teacher/review/tasks' }]
  },
  {
    title: '公告通知',
    defaultOpen: false,
    items: [{ label: '公告管理', to: '/teacher/notices' }]
  },
  {
    title: '反馈管理',
    defaultOpen: false,
    items: [
      { label: '我要反馈', to: '/teacher/feedback/create' },
      { label: '我的反馈', to: '/teacher/feedback/mine' }
    ]
  },
  {
    title: '排名公示',
    defaultOpen: false,
    items: [{ label: '综合排名', to: '/teacher/ranking' }]
  }
]
</script>
