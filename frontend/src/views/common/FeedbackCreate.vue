<template>
  <section class="card">
    <div class="toolbar">
      <div>
        <h2 style="margin: 0;">我要反馈</h2>
        <p class="muted" style="margin-top: 6px;">用于提交系统问题与建议。截图仅支持 JPG/PNG，最多 6 张。</p>
      </div>
      <div class="toolbar-row">
        <button class="btn secondary" type="button" @click="reset" :disabled="submitting">重置</button>
        <button class="btn" type="button" @click="submit" :disabled="submitting">
          {{ submitting ? '提交中...' : '提交反馈' }}
        </button>
      </div>
    </div>

    <div class="grid" style="margin-top: 12px;">
      <label class="field">
        <span class="field-label">标题</span>
        <input v-model.trim="form.title" placeholder="例如：系统无法上传图片" />
      </label>

      <label class="field">
        <span class="field-label">内容</span>
        <textarea v-model.trim="form.content" rows="6" placeholder="请描述问题或建议（可填写复现步骤）"></textarea>
      </label>

      <div class="field">
        <span class="field-label">截图（可选）</span>
        <ImageIdsUploader v-model="form.screenshotFileIds" :max="6" />
      </div>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../../api/http'
import ImageIdsUploader from '../../components/ImageIdsUploader.vue'

const router = useRouter()
const route = useRoute()

const submitting = ref(false)
const form = reactive({
  title: '',
  content: '',
  screenshotFileIds: ''
})

const reset = () => {
  form.title = ''
  form.content = ''
  form.screenshotFileIds = ''
}

const submit = async () => {
  if (!form.title.trim() || !form.content.trim()) {
    alert('标题和内容不能为空')
    return
  }
  submitting.value = true
  try {
    const { data } = await http.post('/feedbacks', {
      title: form.title,
      content: form.content,
      screenshotFileIds: form.screenshotFileIds || ''
    })
    alert('反馈已提交')

    const seg = String(route.path || '').split('/')[1] || 'student'
    const id = data.data?.id
    if (id) {
      router.push(`/${seg}/feedback/${id}`)
    } else if (seg === 'student' || seg === 'teacher') {
      router.push(`/${seg}/feedback/mine`)
    } else {
      router.push(`/${seg}/feedback/handle`)
    }
  } finally {
    submitting.value = false
  }
}
</script>
