<template>
  <section class="grid two">
    <article class="card">
      <p>瑜版挸澧犻悽銊﹀煕閿涙{ realName }}閿涘澖{ role }}閿?/p>
      <div class="grid two">
        <button class="btn" @click="initSubmission">閸掓稑缂?閸旂姾娴囬幋鎴犳畱濞村鐦庨崡?/button>
        <button class="btn secondary" @click="loadScore" :disabled="!submissionId">閸掗攱鏌婄拋鈥冲瀻</button>
      </div>
      <p v-if="submissionId">濞村鐦庨崡鏃綝閿涙{ submissionId }}閿涘瞼濮搁幀渚婄窗{{ status }}</p>
    </article>

    <article class="card">
      <h3>閸掑棙鏆熸０鍕潔</h3>
      <div class="grid two" v-if="score">
        <div>瀵扮柉鍋涢敍姝縶 score.moralRaw }}</div>
        <div>閺呴缚鍋涢敍姝縶 score.intelRaw }}</div>
        <div>娴ｆ捁鍋涢敍姝縶 score.sportRaw }}</div>
        <div>缂囧氦鍋涢敍姝縶 score.artRaw }}</div>
        <div>閸斿疇鍋涢敍姝縶 score.laborRaw }}</div>
        <div>閹鍨庨敍姝縶 score.totalScore }}</div>
      </div>
      <p v-else>閺嗗倹妫ら崚鍡樻殶</p>
    </article>
  </section>

  <section class="card" style="margin-top:16px;">
    <h3>鐠囧墽鈻奸幋鎰摋閿涘牆褰茬紓鏍帆閿?/h3>
    <table class="table">
      <thead>
        <tr><th>鐠囧墽鈻奸崥?/th><th>缁鐎?/th><th>閹存劗鍝?/th><th>鐎涳箑鍨?/th><th>閹垮秳缍?/th></tr>
      </thead>
      <tbody>
        <tr v-for="(c, idx) in courses" :key="idx">
          <td><input v-model="c.courseName" /></td>
          <td>
            <select v-model="c.courseType">
              <option value="REQUIRED">韫囧懍鎱?/option>
              <option value="ELECTIVE">闁鎱?/option>
              <option value="RETAKE">闁插秳鎱?/option>
              <option value="RELEARN">閸愬秳鎱?/option>
            </select>
          </td>
          <td><input type="number" v-model.number="c.score" /></td>
          <td><input type="number" v-model.number="c.credit" step="0.5" /></td>
          <td><button class="btn secondary" @click="courses.splice(idx,1)">閸?/button></td>
        </tr>
      </tbody>
    </table>
    <div style="display:flex;gap:10px;margin-top:10px;">
      <button class="btn secondary" @click="addCourse">閺傛澘顤?/button>
      <button class="btn" @click="saveCourses" :disabled="!submissionId">娣囨繂鐡?/button>
    </div>
  </section>

  <section class="card" style="margin-top:16px;">
    <div class="toolbar">
      <div>
        <h3>濞茶濮╅弶锛勬窗閿涘牊瀵滃Ο鈥虫健閸掑棗灏繅顐㈠晸閿?/h3>
        <p class="muted">濮ｅ繋閲滃ú璇插З閺堚偓婢舵矮绗傛导?6 瀵?JPG/PNG 鐠囦焦妲戦崶鍓у閵嗗倷绗傛导鐘叉倵鐠佹澘绶遍悙鐟板毊閳ユ粈绻氱€涙ǚ鈧縿鈧?/p>
      </div>
      <div class="toolbar-row">
        <button class="btn secondary" @click="saveActivities" :disabled="!submissionId">娣囨繂鐡?/button>
        <button class="btn" @click="submitForm" :disabled="!submissionId">閹绘劒姘︾€光剝鐗?/button>
        <button class="btn ghost" @click="exportFile('DOCX')" :disabled="!submissionId">鐎电厧鍤璚ord</button>
      </div>
    </div>

    <div class="grid two" style="margin-top:12px;">
      <div v-for="b in blocks" :key="b.module" class="subcard">
        <h4 style="margin:0 0 10px 0;">{{ b.label }}</h4>

        <table class="table table-compact">
          <thead>
            <tr>
              <th style="width:160px;">閺嶅洭顣?/th>
              <th>鐠囧瓨妲?/th>
              <th style="width:100px;">閼奉亣鐦庨崚?/th>
              <th style="width:260px;">鐠囦焦妲戦崶鍓у</th>
              <th style="width:70px;">閹垮秳缍?/th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(a, idx) in activitiesByModule[b.module]" :key="a._rowKey || idx">
              <td><input v-model="a.title" placeholder="鐠囩柉绶崗銉︾垼妫? /></td>
              <td><input v-model="a.description" placeholder="閸欘垰锝為崘娆掝嚛閺? /></td>
              <td><input type="number" v-model.number="a.selfScore" step="0.5" min="0" /></td>
              <td>
                <div class="evidence-cell">
                  <div class="toolbar-row" style="gap:8px;">
                    <button class="btn ghost" @click="uploadEvidenceImages(a)" :disabled="!submissionId">娑撳﹣绱堕崶鍓у</button>
                    <span class="muted" style="font-size:12px;">{{ evidenceCount(a) }}/6</span>
                  </div>

                  <div v-if="a._evidenceMetas && a._evidenceMetas.length" class="chip-list">
                    <span v-for="m in a._evidenceMetas" :key="m.id" class="chip">
                      <button class="link" @click="previewEvidence(m.id, a._evidenceMetas)">{{ m.fileName || ('闂勫嫪娆?' + m.id) }}</button>
                      <button class="link danger" @click="removeEvidence(a, m.id)">缁夊娅?/button>
                    </span>
                  </div>
                  <div v-else class="muted" style="font-size:12px;margin-top:6px;">閺堫亙绗傛导?/div>
                </div>
              </td>
              <td><button class="btn danger" @click="removeActivity(b.module, idx)">閸?/button></td>
            </tr>
            <tr v-if="!activitiesByModule[b.module].length">
              <td colspan="5" class="empty">閺嗗倹妫ら弶锛勬窗</td>
            </tr>
          </tbody>
        </table>
        <div style="display:flex;gap:10px;margin-top:10px;">
          <button class="btn secondary" @click="addActivity(b.module)" :disabled="!submissionId">閺傛澘顤?/button>
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
const realName = localStorage.getItem('realName') || '閺堫亞娅ヨぐ?

const submissionId = ref(null)
const status = ref('DRAFT')
const score = ref(null)

const courses = ref([
  { courseName: '妤傛鐡戞禒锝嗘殶', courseType: 'REQUIRED', score: 88, credit: 4 },
  { courseName: '婢堆冾劅娴ｆ捁鍋?, courseType: 'REQUIRED', score: 80, credit: 2 }
])

const blocks = [
  { module: 'MORAL', label: '瀵扮柉鍋? },
  { module: 'INTEL_PRO_INNOV', label: '閺呴缚鍋? },
  { module: 'SPORT_ACTIVITY', label: '娴ｆ捁鍋? },
  { module: 'ART', label: '缂囧氦鍋? },
  { module: 'LABOR', label: '閸斿疇鍋? }
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
  alert('鐠囧墽鈻煎韫箽鐎?)
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
  alert('濞茶濮╁韫箽鐎?)
  await loadDetail()
}

const submitForm = async () => {
  await http.post(`/submissions/${submissionId.value}/submit`)
  alert('瀹稿弶褰佹禍銈咁吀閺?)
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
  const fallback = `缂佺厧鎮庢總鏍ь劅闁叉垹鏁电拠鐤€僟${id}.docx`
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
      a._evidenceMetas = aIds.map((id) => map[id] || evidenceMetaCache[id] || { id, fileName: `闂勫嫪娆?${id}` })
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
    alert('鐠囧嘲鍘涢崚娑樼紦/閸旂姾娴囧ù瀣槑閸?)
    return
  }

  const existing = parseEvidenceIds(activity.evidenceFileIds)
  const files = await pickImages()
  if (!files.length) return

  const validFiles = files.filter(isAllowedImage)
  if (validFiles.length !== files.length) {
    alert('娴犲懏鏁幐浣风瑐娴?JPG/PNG 閸ュ墽澧?)
  }

  if (existing.length + validFiles.length > 6) {
    alert('濮ｅ繋閲滃ú璇插З閺堚偓婢舵矮绗傛导?瀵姾鐦夐弰搴℃禈閻?)
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
  alert('閸ュ墽澧栧韫瑐娴肩媴绱濈拠鐤唶瀵版ぞ绻氱€涙ɑ妞块崝?)
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
    fileNameMap[id] = m.fileName || `闄勪欢#${id}`
  }
  await previewImageById(http, fileId, '璇佹槑鏉愭枡棰勮', galleryIds, fileNameMap)
}
</script>
