<template>
  <div class="shell">
    <header class="topbar">
      <h1>大学生综合测评管理系统</h1>
      <nav>
        <RouterLink to="/student">学生端</RouterLink>
        <RouterLink to="/teacher">辅导员</RouterLink>
        <RouterLink to="/admin">管理员</RouterLink>
        <RouterLink to="/ranking">排名</RouterLink>
      </nav>
    </header>
    <main class="page">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
</script>
