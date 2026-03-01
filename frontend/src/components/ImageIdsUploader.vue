<template>
  <div class="uploader">
    <div class="toolbar-row" style="justify-content: space-between;">
      <div class="toolbar-row" style="gap: 8px;">
        <button v-if="!readonly" class="btn ghost" type="button" @click="uploadImages" :disabled="uploading">
          {{ uploading ? '上传中...' : '上传图片' }}
        </button>
        <span class="muted" style="font-weight: 700;">{{ ids.length }}/{{ max }}</span>
      </div>
      <span v-if="hint" class="muted">{{ hint }}</span>
    </div>

    <div v-if="metas.length" class="chip-list" style="margin-top: 8px;">
      <span v-for="m in metas" :key="m.id" class="chip">
        <button class="link" type="button" @click="preview(m.id)">
          {{ m.fileName || `附件#${m.id}` }}
        </button>
        <button v-if="!readonly" class="link danger" type="button" @click="remove(m.id)">移除</button>
      </span>
    </div>
    <p v-else class="muted" style="margin-top: 8px;">未上传</p>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import http from '../api/http'
import { previewImageById } from '../utils/imagePreview'

const metaCache = Object.create(null)

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  max: {
    type: Number,
    default: 6
  },
  readonly: {
    type: Boolean,
    default: false
  },
  hint: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue'])

const uploading = ref(false)
const metas = ref([])

const parseIds = (raw) => {
  if (!raw) return []
  return raw
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
    .map((s) => Number(s))
    .filter((n) => Number.isFinite(n) && n > 0)
}

const ids = computed(() => parseIds(props.modelValue))

const hydrateMetas = async () => {
  const unique = Array.from(new Set(ids.value))
  if (!unique.length) {
    metas.value = []
    return
  }

  const missing = unique.filter((id) => !metaCache[id])
  if (missing.length) {
    const { data } = await http.post('/files/metas', { ids: missing })
    const list = data.data || []
    for (const m of list) {
      metaCache[m.id] = m
    }
  }

  metas.value = unique.map((id) => metaCache[id] || { id, fileName: `附件#${id}` })
}

watch(
  () => props.modelValue,
  () => {
    hydrateMetas()
  },
  { immediate: true }
)

const isAllowedImage = (file) => {
  if (!file) return false
  const name = (file.name || '').toLowerCase()
  const byExt = name.endsWith('.jpg') || name.endsWith('.jpeg') || name.endsWith('.png')
  const byMime = file.type === 'image/jpeg' || file.type === 'image/png'
  return byExt || byMime
}

const pickImages = () => new Promise((resolve) => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.jpg,.jpeg,.png,image/jpeg,image/png'
  input.multiple = true
  input.onchange = () => resolve(Array.from(input.files || []))
  input.click()
})

const uploadImages = async () => {
  if (props.readonly) return

  const existing = parseIds(props.modelValue)
  const files = await pickImages()
  if (!files.length) return

  const valid = files.filter(isAllowedImage)
  if (valid.length !== files.length) {
    alert('仅支持上传 JPG/PNG 图片')
  }

  if (existing.length + valid.length > props.max) {
    alert(`最多上传 ${props.max} 张图片`)
    return
  }

  uploading.value = true
  try {
    for (const file of valid) {
      const form = new FormData()
      form.append('file', file)
      const { data } = await http.post('/files/upload', form)
      const meta = data.data
      if (meta?.id) {
        existing.push(meta.id)
        metaCache[meta.id] = meta
      }
    }
    emit('update:modelValue', existing.join(','))
    await hydrateMetas()
  } finally {
    uploading.value = false
  }
}

const remove = async (id) => {
  if (props.readonly) return
  const next = parseIds(props.modelValue).filter((n) => n !== id)
  emit('update:modelValue', next.join(','))
  await hydrateMetas()
}

const preview = async (id) => {
  const fileNameMap = {}
  for (const m of metas.value) {
    const mid = Number(m?.id)
    if (!Number.isFinite(mid) || mid <= 0) continue
    fileNameMap[mid] = m.fileName || `附件#${mid}`
  }
  await previewImageById(http, id, '图片预览', ids.value, fileNameMap)
}
</script>

