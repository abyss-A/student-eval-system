<template>
  <section class="grid two">
    <article class="card">
      <h2>瀛︾敓绔～鎶?/h2>
      <p>褰撳墠鐢ㄦ埛锛歿{ realName }}锛坽{ role }}锛?/p>
      <div class="grid two">
        <button class="btn" @click="initSubmission">鍒涘缓/鍔犺浇鎴戠殑娴嬭瘎鍗?/button>
        <button class="btn secondary" @click="loadScore" :disabled="!submissionId">鍒锋柊璁″垎</button>
      </div>
      <p v-if="submissionId">娴嬭瘎鍗旾D锛歿{ submissionId }}锛岀姸鎬侊細{{ status }}</p>
    </article>

    <article class="card">
      <h3>鍒嗘暟棰勮</h3>
      <div class="grid two" v-if="score">
        <div>寰疯偛锛歿{ score.moralRaw }}</div>
        <div>鏅鸿偛锛歿{ score.intelRaw }}</div>
        <div>浣撹偛锛歿{ score.sportRaw }}</div>
        <div>缇庤偛锛歿{ score.artRaw }}</div>
        <div>鍔宠偛锛歿{ score.laborRaw }}</div>
        <div>鎬诲垎锛歿{ score.totalScore }}</div>
      </div>
      <p v-else>鏆傛棤鍒嗘暟</p>
    </article>
  </section>

  <section class="card" style="margin-top:16px;">
    <h3>璇剧▼鎴愮哗锛堝彲缂栬緫锛?/h3>
    <table class="table">
      <thead>
        <tr><th>璇剧▼鍚?/th><th>绫诲瀷</th><th>鎴愮哗</th><th>瀛﹀垎</th><th>鎿嶄綔</th></tr>
      </thead>
      <tbody>
        <tr v-for="(c, idx) in courses" :key="idx">
          <td><input v-model="c.courseName" /></td>
          <td>
            <select v-model="c.courseType">
              <option value="REQUIRED">蹇呬慨</option>
              <option value="ELECTIVE">閫変慨</option>
              <option value="RETAKE">閲嶄慨</option>
              <option value="RELEARN">鍐嶄慨</option>
            </select>
          </td>
          <td><input type="number" v-model.number="c.score" /></td>
          <td><input type="number" v-model.number="c.credit" step="0.5" /></td>
          <td><button class="btn secondary" @click="courses.splice(idx,1)">鍒?/button></td>
        </tr>
      </tbody>
    </table>
    <div style="display:flex;gap:10px;margin-top:10px;">
      <button class="btn secondary" @click="addCourse">鏂板</button>
      <button class="btn" @click="saveCourses" :disabled="!submissionId">淇濆瓨</button>
    </div>
  </section>

  <section class="card" style="margin-top:16px;">
    <div class="toolbar">
      <div>
        <h3>娲诲姩鏉＄洰锛堟寜妯″潡鍒嗗尯濉啓锛?/h3>
        <p class="muted">姣忎釜娲诲姩鏈€澶氫笂浼?6 寮?JPG/PNG 璇佹槑鍥剧墖銆備笂浼犲悗璁板緱鐐瑰嚮鈥滀繚瀛樷€濄€?/p>
      </div>
      <div class="toolbar-row">
        <button class="btn secondary" @click="saveActivities" :disabled="!submissionId">淇濆瓨</button>
        <button class="btn" @click="submitForm" :disabled="!submissionId">鎻愪氦瀹℃牳</button>
        <button class="btn ghost" @click="exportFile('DOCX')" :disabled="!submissionId">瀵煎嚭Word</button>
        <button class="btn ghost" @click="exportFile('PDF')" :disabled="!submissionId">瀵煎嚭PDF</button>
      </div>
    </div>

    <div class="grid two" style="margin-top:12px;">
      <div v-for="b in blocks" :key="b.module" class="subcard">
        <h4 style="margin:0 0 10px 0;">{{ b.label }}</h4>

        <table class="table table-compact">
          <thead>
            <tr>
              <th style="width:160px;">鏍囬</th>
              <th>璇存槑</th>
              <th style="width:100px;">鑷瘎鍒?/th>
              <th style="width:260px;">璇佹槑鍥剧墖</th>
              <th style="width:70px;">鎿嶄綔</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(a, idx) in activitiesByModule[b.module]" :key="a._rowKey || idx">
              <td><input v-model="a.title" placeholder="璇疯緭鍏ユ爣棰? /></td>
              <td><input v-model="a.description" placeholder="鍙～鍐欒鏄? /></td>
              <td><input type="number" v-model.number="a.selfScore" step="0.5" min="0" /></td>
              <td>
                <div class="evidence-cell">
                  <div class="toolbar-row" style="gap:8px;">
                    <button class="btn ghost" @click="uploadEvidenceImages(a)" :disabled="!submissionId">涓婁紶鍥剧墖</button>
                    <span class="muted" style="font-size:12px;">{{ evidenceCount(a) }}/6</span>
                  </div>

                  <div v-if="a._evidenceMetas && a._evidenceMetas.length" class="chip-list">
                    <span v-for="m in a._evidenceMetas" :key="m.id" class="chip">
                      <button class="link" @click="previewEvidence(m.id, a._evidenceMetas)">{{ m.fileName || ('闄勪欢#' + m.id) }}</button>
                      <button class="link danger" @click="removeEvidence(a, m.id)">绉婚櫎</button>
                    </span>
                  </div>
                  <div v-else class="muted" style="font-size:12px;margin-top:6px;">鏈笂浼?/div>
                </div>
              </td>
              <td><button class="btn danger" @click="removeActivity(b.module, idx)">鍒?/button></td>
            </tr>
            <tr v-if="!activitiesByModule[b.module].length">
              <td colspan="5" class="empty">鏆傛棤鏉＄洰</td>
            </tr>
          </tbody>
        </table>
        <div style="display:flex;gap:10px;margin-top:10px;">
          <button class="btn secondary" @click="addActivity(b.module)" :disabled="!submissionId">鏂板</button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import http from '../api/http'
import { previewImageById } from '../utils/imagePreview'

const role = localStorage.getItem('role') || 'UNKNOWN'
const realName = localStorage.getItem('realName') || '鏈櫥褰?

const submissionId = ref(null)
const status = ref('DRAFT')
const score = ref(null)

const courses = ref([
  { courseName: '楂樼瓑浠ｆ暟', courseType: 'REQUIRED', score: 88, credit: 4 },
  { courseName: '澶у浣撹偛', courseType: 'REQUIRED', score: 80, credit: 2 }
])

const blocks = [
  { module: 'MORAL', label: '寰疯偛' },
  { module: 'INTEL_PRO_INNOV', label: '鏅鸿偛' },
  { module: 'SPORT_ACTIVITY', label: '浣撹偛' },
  { module: 'ART', label: '缇庤偛' },
  { module: 'LABOR', label: '鍔宠偛' }
]

const activitiesByModule = reactive({
  MORAL: [],
  INTEL_PRO_INNOV: [],
  SPORT_ACTIVITY: [],
  ART: [],
  LABOR: []
})

const evidenceMetaCache = reactive({})

const initSubmission = async () => {
  const { data } = await http.post('/submissions')
  submissionId.value = data.data.id
  status.value = data.data.status
  await loadDetail()
}

const loadDetail = async () => {
  if (!submissionId.value) return
  const { data } = await http.get(`/submissions/${submissionId.value}`)
  const d = data.data
  courses.value = d.courses?.length ? d.courses : courses.value
  resetActivities()
  const list = d.activities || []
  for (const a of list) {
    const module = (a.moduleType || 'MORAL').toUpperCase()
    const normalized = normalizeActivity(a, module)
    if (activitiesByModule[module]) activitiesByModule[module].push(normalized)
    else activitiesByModule.MORAL.push(normalized)
  }
  await hydrateEvidenceMetas()
  status.value = d.submission.status
}

const loadScore = async () => {
  if (!submissionId.value) return
  const { data } = await http.get(`/submissions/${submissionId.value}/score`)
  score.value = data.data
}

const addCourse = () => {
  courses.value.push({ courseName: '', courseType: 'REQUIRED', score: 0, credit: 0 })
}
const addActivity = (module) => {
  const key = (module || 'MORAL').toUpperCase()
  if (!activitiesByModule[key]) return
  activitiesByModule[key].push(
    normalizeActivity(
      { moduleType: key, title: '', description: '', selfScore: 0, evidenceFileIds: '' },
      key
    )
  )
}

const removeActivity = (module, idx) => {
  const key = (module || 'MORAL').toUpperCase()
  if (!activitiesByModule[key]) return
  activitiesByModule[key].splice(idx, 1)
}

const saveCourses = async () => {
  await http.put(`/submissions/${submissionId.value}/courses/batch`, { items: courses.value })
  alert('璇剧▼宸蹭繚瀛?)
}

const saveActivities = async () => {
  const items = []
  for (const b of blocks) {
    const list = activitiesByModule[b.module] || []
    for (const a of list) {
      items.push({
        id: a.id,
        moduleType: b.module,
        title: a.title,
        description: a.description,
        selfScore: a.selfScore,
        evidenceFileIds: a.evidenceFileIds || ''
      })
    }
  }
  await http.put(`/submissions/${submissionId.value}/activities/batch`, { items })
  alert('娲诲姩宸蹭繚瀛?)
  await loadDetail()
}

const submitForm = async () => {
  await http.post(`/submissions/${submissionId.value}/submit`)
  alert('宸叉彁浜ゅ鏍?)
  await loadDetail()
  await loadScore()
}

const exportFile = async (format) => {
  const resp = await http.post(`/submissions/${submissionId.value}/report/export`, { format }, { responseType: 'blob' })
  const blob = new Blob([resp.data])
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = resolveDownloadName(resp, format, submissionId.value)
  a.click()
  URL.revokeObjectURL(url)
}

const resolveDownloadName = (resp, format, id) => {
  const fallback = format === 'PDF'
    ? `缁煎悎濂栧閲戠敵璇疯〃_${id}.pdf`
    : `缁煎悎濂栧閲戠敵璇疯〃_${id}.docx`
  const disposition = resp?.headers?.['content-disposition'] || resp?.headers?.['Content-Disposition']
  if (!disposition) return fallback

  const utf8Match = disposition.match(/filename\*\s*=\s*UTF-8''([^;]+)/i)
  if (utf8Match && utf8Match[1]) {
    try {
      return decodeURIComponent(utf8Match[1])
    } catch (_) {
      return utf8Match[1]
    }
  }

  const normalMatch = disposition.match(/filename\s*=\s*\"?([^\";]+)\"?/i)
  if (normalMatch && normalMatch[1]) {
    return normalMatch[1]
  }
  return fallback
}

const resetActivities = () => {
  activitiesByModule.MORAL = []
  activitiesByModule.INTEL_PRO_INNOV = []
  activitiesByModule.SPORT_ACTIVITY = []
  activitiesByModule.ART = []
  activitiesByModule.LABOR = []
}

const normalizeActivity = (a, module) => {
  return {
    ...a,
    moduleType: module,
    title: a.title || '',
    description: a.description || '',
    selfScore: Number(a.selfScore ?? 0),
    evidenceFileIds: a.evidenceFileIds || '',
    _rowKey: a.id ? `act_${a.id}` : `tmp_${module}_${Date.now()}_${Math.random()}`,
    _evidenceMetas: []
  }
}

const parseEvidenceIds = (raw) => {
  if (!raw) return []
  return raw
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
    .map((s) => Number(s))
    .filter((n) => Number.isFinite(n) && n > 0)
}

const evidenceCount = (activity) => parseEvidenceIds(activity?.evidenceFileIds).length

const hydrateEvidenceMetas = async () => {
  const ids = []
  for (const b of blocks) {
    for (const a of activitiesByModule[b.module] || []) {
      ids.push(...parseEvidenceIds(a.evidenceFileIds))
    }
  }

  const uniqueIds = Array.from(new Set(ids))
  if (!uniqueIds.length) {
    for (const b of blocks) {
      for (const a of activitiesByModule[b.module] || []) {
        a._evidenceMetas = []
      }
    }
    return
  }

  const { data } = await http.post('/files/metas', { ids: uniqueIds })
  const metas = data.data || []
  const map = {}
  for (const m of metas) {
    map[m.id] = m
    evidenceMetaCache[m.id] = m
  }

  for (const b of blocks) {
    for (const a of activitiesByModule[b.module] || []) {
      const aIds = parseEvidenceIds(a.evidenceFileIds)
      a._evidenceMetas = aIds.map((id) => map[id] || evidenceMetaCache[id] || { id, fileName: `闄勪欢#${id}` })
    }
  }
}

const pickImages = () => {
  return new Promise((resolve) => {
    const input = document.createElement('input')
    input.type = 'file'
    input.accept = '.jpg,.jpeg,.png,image/jpeg,image/png'
    input.multiple = true
    input.onchange = () => resolve(Array.from(input.files || []))
    input.click()
  })
}

const isAllowedImage = (file) => {
  if (!file) return false
  const name = (file.name || '').toLowerCase()
  const byExt = name.endsWith('.jpg') || name.endsWith('.jpeg') || name.endsWith('.png')
  const byMime = file.type === 'image/jpeg' || file.type === 'image/png'
  return byExt || byMime
}

const uploadEvidenceImages = async (activity) => {
  if (!submissionId.value) {
    alert('璇峰厛鍒涘缓/鍔犺浇娴嬭瘎鍗?)
    return
  }

  const existing = parseEvidenceIds(activity.evidenceFileIds)
  const files = await pickImages()
  if (!files.length) return

  const validFiles = files.filter(isAllowedImage)
  if (validFiles.length !== files.length) {
    alert('浠呮敮鎸佷笂浼?JPG/PNG 鍥剧墖')
  }

  if (existing.length + validFiles.length > 6) {
    alert('姣忎釜娲诲姩鏈€澶氫笂浼?寮犺瘉鏄庡浘鐗?)
    return
  }

  for (const file of validFiles) {
    const form = new FormData()
    form.append('file', file)
    const { data } = await http.post('/files/upload', form)
    const meta = data.data
    if (meta?.id) {
      existing.push(meta.id)
      evidenceMetaCache[meta.id] = meta
    }
  }

  activity.evidenceFileIds = existing.join(',')
  await hydrateEvidenceMetas()
  alert('鍥剧墖宸蹭笂浼狅紝璇疯寰椾繚瀛樻椿鍔?)
}

const removeEvidence = async (activity, fileId) => {
  const remaining = parseEvidenceIds(activity.evidenceFileIds).filter((id) => id !== fileId)
  activity.evidenceFileIds = remaining.join(',')
  await hydrateEvidenceMetas()
}

const previewEvidence = async (fileId, metas = []) => {
  const galleryIds = (metas || [])
    .map((m) => Number(m?.id))
    .filter((id) => Number.isFinite(id) && id > 0)
  const fileNameMap = {}
  for (const m of metas || []) {
    const id = Number(m?.id)
    if (!Number.isFinite(id) || id <= 0) continue
    fileNameMap[id] = m.fileName || `附件#${id}`
  }
  await previewImageById(http, fileId, '证明材料预览', galleryIds, fileNameMap)
}
</script>
