<template>
  <BaseLayout :menu-groups="menuGroups" />
</template>

<script setup>
import BaseLayout from './BaseLayout.vue'

const menuGroups = [
  {
    title: '测评查看',
    items: [{ label: '测评单查看', to: '/admin/submissions' }]
  },
  {
    title: '权限管理',
    items: [
      { label: '班级权限管理', to: '/admin/counselor/scopes' },
      { label: '账号管理', to: '/admin/accounts' }
    ]
  },
  {
    title: '公告通知',
    items: [{ label: '公告管理', to: '/admin/notices' }]
  },
  {
    title: '反馈管理',
    items: [{ label: '反馈处理', to: '/admin/feedback/handle' }]
  },
  {
    title: '排名公示',
    items: [{ label: '综合排名', to: '/admin/ranking' }]
  }
]
</script>
