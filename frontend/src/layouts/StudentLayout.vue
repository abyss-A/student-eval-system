<template>
  <BaseLayout :menu-groups="menuGroups" />
</template>

<script setup>
import BaseLayout from './BaseLayout.vue'

const menuGroups = [
  {
    title: '测评填报',
    items: [
      { label: '课程成绩', to: '/student/eval/course' },
      { label: '德育', to: '/student/eval/moral' },
      { label: '智育', to: '/student/eval/intel' },
      { label: '体育', to: '/student/eval/sport' },
      { label: '美育', to: '/student/eval/art' },
      { label: '劳育', to: '/student/eval/labor' },
      { label: '综合成绩与提交', to: '/student/eval/submit' }
    ]
  },
  {
    title: '公告通知',
    items: [{ label: '公告列表', to: '/student/notices' }]
  },
  {
    title: '反馈管理',
    items: [
      { label: '我要反馈', to: '/student/feedback/create' },
      { label: '我的反馈', to: '/student/feedback/mine' }
    ]
  },
  {
    title: '排名公示',
    items: [{ label: '综合排名', to: '/student/ranking' }]
  }
]
</script>
