<template>
  <section class="card semester-page page-shell">
    <p class="muted semester-note">
      学期用于区分每学期的填报、审核与综合排名。新建学期默认不立即生效；切换当前学期前请确保本学期待审核已清零。评分配置保存后不自动重算，如需让排名按新配置更新，请在评分配置中手动重算本学期成绩。
    </p>

    <div v-if="noticeText" class="semester-notice" :class="noticeType">
      <span class="semester-notice-label">{{ noticeType === 'error' ? '处理失败' : '操作完成' }}</span>
      <span>{{ noticeText }}</span>
    </div>

    <div class="semester-summary">
      <div class="semester-summary-left">
        <div class="semester-summary-row">
          <span class="semester-summary-label">当前学期</span>
          <span class="semester-summary-value">{{ activeSemester?.name || '未设置' }}</span>
          <el-tag v-if="activeSemester?.name" type="success" effect="light" style="margin-left: 8px;">当前</el-tag>
        </div>
        <div class="semester-summary-row">
          <span class="semester-summary-label">待审核（SUBMITTED）</span>
          <span class="semester-summary-value">{{ submittedPendingCount }}</span>
          <span class="muted" style="margin-left: 8px;">份</span>
        </div>
      </div>
      <div class="semester-summary-actions">
        <el-button type="default" :disabled="loading" @click="load">刷新</el-button>
        <el-button type="primary" :disabled="loading" @click="openCreateDialog">新建学期</el-button>
      </div>
    </div>

    <el-alert
      v-if="Number(submittedPendingCount) > 0"
      type="warning"
      :closable="false"
      show-icon
      title="当前学期仍存在待审核测评单，已禁止切换学期。请先完成本学期审核后再切换。"
      class="semester-warning"
    />

    <div v-if="errorMsg" class="card semester-error-card">
      <el-alert type="error" :closable="false" title="加载失败" />
      <div class="muted semester-error-text">{{ errorMsg }}</div>
      <div class="toolbar-row semester-error-actions">
        <el-button type="default" @click="load" :loading="loading">重试</el-button>
      </div>
    </div>

    <div class="table-shell semester-table-shell">
      <div class="table-scroll-main">
        <table
          class="table table-sticky table-fixed-right semester-table"
          style="--sticky-action-w: 188px; --fixed-action-btn-w: 82px; --fixed-action-gap: 4px; --fixed-action-padding-x: 4px;"
        >
          <thead>
            <tr>
              <th>学期名称</th>
              <th>年份</th>
              <th>季节</th>
              <th>状态</th>
              <th>创建时间</th>
              <th class="col-action">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in semesters" :key="item.id">
              <td>
                <span class="semester-name">{{ item.name || '-' }}</span>
              </td>
              <td>{{ item.year ?? '-' }}</td>
              <td>{{ seasonLabel(item.term) }}</td>
              <td>
                <el-tag v-if="Number(item.isActive) === 1" type="success" effect="light">当前</el-tag>
                <el-tag v-else type="info" effect="plain">非当前</el-tag>
              </td>
              <td class="cell-secondary">{{ formatDate(item.createdAt) }}</td>
              <td class="col-action">
                <div class="action-row inline-actions">
                  <el-button size="small" type="default" :disabled="loading" @click="openConfigDialog(item)">评分配置</el-button>
                  <el-dropdown trigger="click" @command="(command) => handleRowCommand(command, item)">
                    <el-button size="small" type="default" :loading="actionKey === `row_${item.id}`" :disabled="loading">
                      更多
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item
                          command="activate"
                          :disabled="Number(item.isActive) === 1 || Number(submittedPendingCount) > 0"
                        >设为当前</el-dropdown-item>
                        <el-dropdown-item command="rename">编辑名称</el-dropdown-item>
                        <el-dropdown-item command="delete" :disabled="Number(item.isActive) === 1">删除学期</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </td>
            </tr>
            <tr v-if="!semesters.length">
              <td colspan="6" class="empty">暂无学期数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </section>

  <el-dialog v-model="createDialogOpen" width="520px" class="semester-create-dialog" :before-close="handleCreateBeforeClose">
    <template #header>
      <div class="semester-dialog-head">
        <h3 class="semester-dialog-title">新建学期</h3>
        <el-tag type="info" effect="plain">默认不立即生效</el-tag>
      </div>
    </template>

    <div class="semester-form">
      <div class="semester-form-row">
        <span class="semester-form-label">年份</span>
        <el-input-number v-model="createForm.year" :min="2000" :max="2100" :disabled="creating" controls-position="right" />
      </div>

      <div class="semester-form-row">
        <span class="semester-form-label">季节</span>
        <el-radio-group v-model="createForm.season" :disabled="creating">
          <el-radio-button label="SPRING">春季</el-radio-button>
          <el-radio-button label="AUTUMN">秋季</el-radio-button>
        </el-radio-group>
      </div>

      <div class="semester-form-row">
        <span class="semester-form-label">名称</span>
        <el-input
          v-model.trim="createForm.name"
          placeholder="例如：2026年春季学期"
          :disabled="creating"
          @input="onCreateNameInput"
        />
      </div>

      <div v-if="createError" class="semester-form-error">{{ createError }}</div>
    </div>

    <template #footer>
      <div class="semester-dialog-footer">
        <el-button type="default" :disabled="creating" @click="closeCreateDialog">取消</el-button>
        <el-button type="primary" :loading="creating" :disabled="creating" @click="createSemester">
          {{ creating ? '创建中...' : '创建' }}
        </el-button>
      </div>
    </template>
  </el-dialog>

  <el-dialog v-model="configDialogOpen" width="760px" class="semester-config-dialog" :before-close="handleConfigBeforeClose">
    <template #header>
      <div class="semester-dialog-head">
        <h3 class="semester-dialog-title">评分配置</h3>
        <el-tag v-if="configSemester?.name" type="info" effect="plain">{{ configSemester?.name }}</el-tag>
      </div>
    </template>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="保存评分配置不会自动重算历史测评单与排名；如需让排名按新配置更新，请点击“重算本学期成绩”。"
      class="semester-config-tip"
    />

    <div v-if="configError" class="semester-form-error">{{ configError }}</div>

    <div class="semester-config-grid">
      <div class="semester-config-section">
        <h4 class="semester-config-title">权重（0~1）</h4>
        <div class="semester-form-row">
          <span class="semester-form-label">德育</span>
          <el-input-number v-model="configForm.wMoral" :min="0" :max="1" :step="0.01" :precision="4" controls-position="right" :disabled="configLoading || configSaving || recalculating" />
        </div>
        <div class="semester-form-row">
          <span class="semester-form-label">智育</span>
          <el-input-number v-model="configForm.wIntel" :min="0" :max="1" :step="0.01" :precision="4" controls-position="right" :disabled="configLoading || configSaving || recalculating" />
        </div>
        <div class="semester-form-row">
          <span class="semester-form-label">体育</span>
          <el-input-number v-model="configForm.wSport" :min="0" :max="1" :step="0.01" :precision="4" controls-position="right" :disabled="configLoading || configSaving || recalculating" />
        </div>
        <div class="semester-form-row">
          <span class="semester-form-label">美育</span>
          <el-input-number v-model="configForm.wArt" :min="0" :max="1" :step="0.01" :precision="4" controls-position="right" :disabled="configLoading || configSaving || recalculating" />
        </div>
        <div class="semester-form-row">
          <span class="semester-form-label">劳育</span>
          <el-input-number v-model="configForm.wLabor" :min="0" :max="1" :step="0.01" :precision="4" controls-position="right" :disabled="configLoading || configSaving || recalculating" />
        </div>

        <p class="muted semester-config-sum" :class="{ invalid: !isWeightSumValid }">
          权重合计：{{ weightSumText }} <span v-if="!isWeightSumValid">（需为 1）</span>
        </p>
      </div>

      <div class="semester-config-section">
        <h4 class="semester-config-title">上限</h4>
        <div class="semester-form-row">
          <span class="semester-form-label">德育</span>
          <el-input-number v-model="configForm.capMoral" :min="0" :max="1000" :step="1" :precision="2" controls-position="right" :disabled="configLoading || configSaving || recalculating" />
        </div>
        <div class="semester-form-row">
          <span class="semester-form-label wide">创新活动上限（智育活动分池）</span>
          <el-input-number v-model="configForm.capIntel" :min="0" :max="1000" :step="1" :precision="2" controls-position="right" :disabled="configLoading || configSaving || recalculating" />
        </div>
        <div class="semester-form-row">
          <span class="semester-form-label wide">体育活动上限（除大学体育）</span>
          <el-input-number v-model="configForm.capSport" :min="0" :max="1000" :step="1" :precision="2" controls-position="right" :disabled="configLoading || configSaving || recalculating" />
        </div>
        <div class="semester-form-row">
          <span class="semester-form-label">美育</span>
          <el-input-number v-model="configForm.capArt" :min="0" :max="1000" :step="1" :precision="2" controls-position="right" :disabled="configLoading || configSaving || recalculating" />
        </div>
        <div class="semester-form-row">
          <span class="semester-form-label">劳育</span>
          <el-input-number v-model="configForm.capLabor" :min="0" :max="1000" :step="1" :precision="2" controls-position="right" :disabled="configLoading || configSaving || recalculating" />
        </div>
      </div>

      <div class="semester-config-section">
        <h4 class="semester-config-title">智育细则（0~1）</h4>
        <div class="semester-form-row">
          <span class="semester-form-label wide">课程平均占比</span>
          <el-input-number v-model="configForm.intelCourseRatio" :min="0" :max="1" :step="0.01" :precision="4" controls-position="right" :disabled="configLoading || configSaving || recalculating" />
        </div>
        <div class="semester-form-row">
          <span class="semester-form-label wide">创新活动占比</span>
          <el-input-number v-model="configForm.intelInnovationRatio" :min="0" :max="1" :step="0.01" :precision="4" controls-position="right" :disabled="configLoading || configSaving || recalculating" />
        </div>
        <p class="muted semester-config-sum" :class="{ invalid: !isIntelRatioSumValid }">
          合计：{{ intelRatioSumText }} <span v-if="!isIntelRatioSumValid">（需为 1）</span>
        </p>
      </div>

      <div class="semester-config-section">
        <h4 class="semester-config-title">体育细则（0~1）</h4>
        <div class="semester-form-row">
          <span class="semester-form-label wide">大学体育占比</span>
          <el-input-number v-model="configForm.sportUniversityPeRatio" :min="0" :max="1" :step="0.01" :precision="4" controls-position="right" :disabled="configLoading || configSaving || recalculating" />
        </div>
        <div class="semester-form-row">
          <span class="semester-form-label wide">体育活动占比（除大学体育）</span>
          <el-input-number v-model="configForm.sportActivityRatio" :min="0" :max="1" :step="0.01" :precision="4" controls-position="right" :disabled="configLoading || configSaving || recalculating" />
        </div>
        <p class="muted semester-config-sum" :class="{ invalid: !isSportRatioSumValid }">
          合计：{{ sportRatioSumText }} <span v-if="!isSportRatioSumValid">（需为 1）</span>
        </p>
      </div>
    </div>

    <template #footer>
      <div class="semester-dialog-footer">
        <el-button type="default" :disabled="configSaving || recalculating" @click="closeConfigDialog">关闭</el-button>
        <el-button type="default" :loading="recalculating" :disabled="configLoading || configSaving || !configSemester?.id" @click="recalculateSemesterScores">
          重算本学期成绩
        </el-button>
        <el-button type="primary" :loading="configSaving" :disabled="configLoading || recalculating || !configSemester?.id || !isConfigValid" @click="saveScoringConfig">
          保存配置
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import http from '../../api/http'

const withTimeout = async (promise, ms, message) => {
  let timer = null
  try {
    return await Promise.race([
      promise,
      new Promise((_, reject) => {
        timer = setTimeout(() => reject(new Error(message || '请求超时')), ms)
      })
    ])
  } finally {
    if (timer) clearTimeout(timer)
  }
}

const loading = ref(false)
const errorMsg = ref('')
const semesters = ref([])
const activeSemester = ref(null)
const submittedPendingCount = ref(0)

const noticeText = ref('')
const noticeType = ref('success')
const actionKey = ref('')

const showNotice = (text, type = 'success') => {
  noticeText.value = text
  noticeType.value = type
}

const clearNotice = () => {
  noticeText.value = ''
  noticeType.value = 'success'
}

const formatDate = (raw) => {
  if (!raw) return '-'
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) return raw
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const seasonLabel = (term) => {
  const value = Number(term)
  if (value === 1) return '春季'
  if (value === 2) return '秋季'
  return '-'
}

const load = async (opts = {}) => {
  loading.value = true
  errorMsg.value = ''
  if (opts?.clearNotice !== false) {
    clearNotice()
  }
  try {
    const { data } = await http.get('/admin/semesters', { meta: { silent: true } })
    const payload = data.data || {}
    semesters.value = payload.semesters || []
    activeSemester.value = payload.activeSemester || null
    submittedPendingCount.value = Number(payload.submittedPendingCount || 0)
  } catch (e) {
    errorMsg.value = e?.userMessage || e?.message || '加载失败'
    semesters.value = []
    activeSemester.value = null
    submittedPendingCount.value = 0
  } finally {
    loading.value = false
  }
}

const createDialogOpen = ref(false)
const creating = ref(false)
const createError = ref('')
const createAutoName = ref(true)
const createForm = reactive({
  year: new Date().getFullYear(),
  season: 'SPRING',
  name: ''
})

const defaultName = computed(() => {
  const year = Number(createForm.year) || new Date().getFullYear()
  const seasonText = createForm.season === 'AUTUMN' ? '秋季' : '春季'
  return `${year}年${seasonText}学期`
})

watch(
  () => [createForm.year, createForm.season],
  () => {
    if (createAutoName.value) {
      createForm.name = defaultName.value
    }
  }
)

const onCreateNameInput = () => {
  createAutoName.value = false
}

const openCreateDialog = () => {
  createError.value = ''
  createAutoName.value = true
  createForm.year = new Date().getFullYear()
  createForm.season = 'SPRING'
  createForm.name = defaultName.value
  createDialogOpen.value = true
}

const closeCreateDialog = () => {
  if (creating.value) return
  createDialogOpen.value = false
}

const handleCreateBeforeClose = () => {
  closeCreateDialog()
}

const createSemester = async () => {
  if (creating.value) return
  createError.value = ''
  const payload = {
    year: Number(createForm.year),
    season: createForm.season,
    name: String(createForm.name || '').trim()
  }
  if (!payload.year || payload.year < 2000 || payload.year > 2100) {
    createError.value = '年份不合法'
    return
  }
  if (!payload.name) {
    createError.value = '学期名称不能为空'
    return
  }

  creating.value = true
  try {
    await http.post('/admin/semesters', payload, { meta: { silent: true } })
    showNotice('新学期已创建（未设为当前）', 'success')
    createDialogOpen.value = false
    await load({ clearNotice: false })
  } catch (e) {
    createError.value = e?.userMessage || e?.message || '创建失败'
  } finally {
    creating.value = false
  }
}

const configDialogOpen = ref(false)
const configSemester = ref(null)
const configLoading = ref(false)
const configSaving = ref(false)
const recalculating = ref(false)
const configError = ref('')
const configForm = reactive({
  wMoral: 0.15,
  wIntel: 0.6,
  wSport: 0.1,
  wArt: 0.075,
  wLabor: 0.075,
  intelCourseRatio: 0.85,
  intelInnovationRatio: 0.15,
  sportUniversityPeRatio: 0.85,
  sportActivityRatio: 0.15,
  capMoral: 100,
  capIntel: 100,
  capSport: 100,
  capArt: 100,
  capLabor: 100
})

const weightSum = computed(() => {
  return Number(configForm.wMoral || 0)
    + Number(configForm.wIntel || 0)
    + Number(configForm.wSport || 0)
    + Number(configForm.wArt || 0)
    + Number(configForm.wLabor || 0)
})

const isWeightSumValid = computed(() => Math.abs(weightSum.value - 1) <= 1e-6)
const weightSumText = computed(() => weightSum.value.toFixed(6))

const intelRatioSum = computed(() => Number(configForm.intelCourseRatio || 0) + Number(configForm.intelInnovationRatio || 0))
const sportRatioSum = computed(() => Number(configForm.sportUniversityPeRatio || 0) + Number(configForm.sportActivityRatio || 0))
const isIntelRatioSumValid = computed(() => Math.abs(intelRatioSum.value - 1) <= 1e-6)
const isSportRatioSumValid = computed(() => Math.abs(sportRatioSum.value - 1) <= 1e-6)
const intelRatioSumText = computed(() => intelRatioSum.value.toFixed(6))
const sportRatioSumText = computed(() => sportRatioSum.value.toFixed(6))
const isConfigValid = computed(() => isWeightSumValid.value && isIntelRatioSumValid.value && isSportRatioSumValid.value)

const fillConfigForm = (cfg) => {
  configForm.wMoral = Number(cfg?.wMoral ?? cfg?.w_moral ?? configForm.wMoral)
  configForm.wIntel = Number(cfg?.wIntel ?? cfg?.w_intel ?? configForm.wIntel)
  configForm.wSport = Number(cfg?.wSport ?? cfg?.w_sport ?? configForm.wSport)
  configForm.wArt = Number(cfg?.wArt ?? cfg?.w_art ?? configForm.wArt)
  configForm.wLabor = Number(cfg?.wLabor ?? cfg?.w_labor ?? configForm.wLabor)
  configForm.intelCourseRatio = Number(cfg?.intelCourseRatio ?? cfg?.intel_course_ratio ?? configForm.intelCourseRatio)
  configForm.intelInnovationRatio = Number(cfg?.intelInnovationRatio ?? cfg?.intel_innovation_ratio ?? configForm.intelInnovationRatio)
  configForm.sportUniversityPeRatio = Number(cfg?.sportUniversityPeRatio ?? cfg?.sport_university_pe_ratio ?? configForm.sportUniversityPeRatio)
  configForm.sportActivityRatio = Number(cfg?.sportActivityRatio ?? cfg?.sport_activity_ratio ?? configForm.sportActivityRatio)
  configForm.capMoral = Number(cfg?.capMoral ?? cfg?.cap_moral ?? configForm.capMoral)
  configForm.capIntel = Number(cfg?.capIntel ?? cfg?.cap_intel ?? configForm.capIntel)
  configForm.capSport = Number(cfg?.capSport ?? cfg?.cap_sport ?? configForm.capSport)
  configForm.capArt = Number(cfg?.capArt ?? cfg?.cap_art ?? configForm.capArt)
  configForm.capLabor = Number(cfg?.capLabor ?? cfg?.cap_labor ?? configForm.capLabor)
}

const openConfigDialog = async (item) => {
  if (!item?.id) return
  configError.value = ''
  configSemester.value = item
  configDialogOpen.value = true

  configLoading.value = true
  try {
    const { data } = await withTimeout(
      http.get(`/admin/semesters/${item.id}/scoring-config`, { meta: { silent: true } }),
      8000,
      '评分配置加载超时，请稍后重试'
    )
    fillConfigForm(data.data || {})
  } catch (e) {
    configError.value = e?.userMessage || e?.message || '评分配置加载失败'
  } finally {
    configLoading.value = false
  }
}

const closeConfigDialog = () => {
  if (configSaving.value || recalculating.value) return
  configDialogOpen.value = false
  configSemester.value = null
  configError.value = ''
}

const handleConfigBeforeClose = () => {
  closeConfigDialog()
}

const saveScoringConfig = async () => {
  if (!configSemester.value?.id) return
  if (configSaving.value) return
  configError.value = ''
  if (!isWeightSumValid.value) {
    configError.value = `五项权重之和需为 1，当前为 ${weightSum.value}`
    return
  }
  if (!isIntelRatioSumValid.value) {
    configError.value = `智育二级占比之和需为 1，当前为 ${intelRatioSum.value}`
    return
  }
  if (!isSportRatioSumValid.value) {
    configError.value = `体育二级占比之和需为 1，当前为 ${sportRatioSum.value}`
    return
  }

  const payload = {
    wMoral: Number(configForm.wMoral),
    wIntel: Number(configForm.wIntel),
    wSport: Number(configForm.wSport),
    wArt: Number(configForm.wArt),
    wLabor: Number(configForm.wLabor),
    intelCourseRatio: Number(configForm.intelCourseRatio),
    intelInnovationRatio: Number(configForm.intelInnovationRatio),
    sportUniversityPeRatio: Number(configForm.sportUniversityPeRatio),
    sportActivityRatio: Number(configForm.sportActivityRatio),
    capMoral: Number(configForm.capMoral),
    capIntel: Number(configForm.capIntel),
    capSport: Number(configForm.capSport),
    capArt: Number(configForm.capArt),
    capLabor: Number(configForm.capLabor)
  }

  configSaving.value = true
  try {
    await http.put(`/admin/semesters/${configSemester.value.id}/scoring-config`, payload, { meta: { silent: true } })
    showNotice('评分配置已保存（未重算）', 'success')
  } catch (e) {
    configError.value = e?.userMessage || e?.message || '保存失败'
  } finally {
    configSaving.value = false
  }
}

const recalculateSemesterScores = async () => {
  if (!configSemester.value?.id) return
  if (recalculating.value) return
  configError.value = ''

  const name = String(configSemester.value?.name || '').trim() || '该学期'
  try {
    await ElMessageBox.confirm(`确定要重算“${name}”的所有已提交测评单分数吗？此操作可能需要一点时间。`, '确认重算', {
      type: 'warning',
      confirmButtonText: '开始重算',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  const start = Date.now()
  recalculating.value = true
  try {
    const { data } = await http.post(`/admin/semesters/${configSemester.value.id}/recalculate`, null, { meta: { silent: true } })
    const res = data.data || {}
    const ms = Date.now() - start
    showNotice(`重算完成：处理 ${res.total ?? 0}，更新 ${res.updated ?? 0}（${ms}ms）`, 'success')
    await load({ clearNotice: false })
  } catch (e) {
    configError.value = e?.userMessage || e?.message || '重算失败'
  } finally {
    recalculating.value = false
  }
}

const activate = async (item) => {
  if (!item?.id) return
  if (loading.value) return
  if (Number(submittedPendingCount.value) > 0) {
    showNotice(`当前学期仍有${submittedPendingCount.value}份待审核，无法切换`, 'error')
    return
  }

  const name = String(item?.name || '').trim() || '目标学期'
  try {
    await ElMessageBox.confirm(`确定将“${name}”设为当前学期吗？切换后学生将开始该学期填报。`, '确认切换', {
      type: 'warning',
      confirmButtonText: '确认切换',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  actionKey.value = `activate_${item.id}`
  try {
    await http.put(`/admin/semesters/${item.id}/active`, null, { meta: { silent: true } })
    showNotice('已切换当前学期', 'success')
    await load({ clearNotice: false })
  } catch (e) {
    showNotice(e?.userMessage || e?.message || '切换失败', 'error')
  } finally {
    actionKey.value = ''
  }
}

const renameSemester = async (item) => {
  if (!item?.id) return
  const current = String(item?.name || '').trim()
  let nextName = ''
  try {
    const { value } = await ElMessageBox.prompt('请输入新的学期名称', '编辑名称', {
      inputValue: current,
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：2026年春季学期',
      inputValidator: (val) => {
        const name = String(val || '').trim()
        if (!name) return '学期名称不能为空'
        if (name.length > 64) return '学期名称不能超过64位'
        return true
      }
    })
    nextName = String(value || '').trim()
  } catch {
    return
  }

  actionKey.value = `row_${item.id}`
  try {
    await http.put(`/admin/semesters/${item.id}`, { name: nextName }, { meta: { silent: true } })
    showNotice('学期名称已更新', 'success')
    await load({ clearNotice: false })
  } catch (e) {
    showNotice(e?.userMessage || e?.message || '更新失败', 'error')
  } finally {
    actionKey.value = ''
  }
}

const deleteSemester = async (item) => {
  if (!item?.id) return
  const name = String(item?.name || '').trim() || '目标学期'
  try {
    await ElMessageBox.confirm(`确定删除“${name}”吗？仅当该学期下没有任何测评单时才允许删除。`, '确认删除', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  actionKey.value = `row_${item.id}`
  try {
    await http.delete(`/admin/semesters/${item.id}`, { meta: { silent: true } })
    showNotice('学期已删除', 'success')
    await load({ clearNotice: false })
  } catch (e) {
    showNotice(e?.userMessage || e?.message || '删除失败', 'error')
  } finally {
    actionKey.value = ''
  }
}

const handleRowCommand = (command, item) => {
  switch (command) {
    case 'activate':
      activate(item)
      break
    case 'rename':
      renameSemester(item)
      break
    case 'delete':
      deleteSemester(item)
      break
    default:
      break
  }
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.semester-note {
  margin-bottom: 10px;
}

.semester-notice {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 10px 12px;
  border-radius: 10px;
  margin-bottom: 10px;
  border: 1px solid transparent;
}

.semester-notice.success {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: #1e3a8a;
}

.semester-notice.error {
  background: #fef2f2;
  border-color: #fecaca;
  color: #7f1d1d;
}

.semester-notice-label {
  font-weight: 600;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.06);
}

.semester-summary {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #ffffff;
  margin-bottom: 10px;
}

.semester-summary-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 2px 0;
}

.semester-summary-label {
  color: #64748b;
  font-size: 12px;
  min-width: 120px;
}

.semester-summary-value {
  font-weight: 600;
  color: #0f172a;
}

.semester-summary-actions {
  display: flex;
  gap: 8px;
}

.semester-warning {
  margin-bottom: 10px;
}

.semester-error-card {
  margin-bottom: 10px;
  border-color: #fecaca;
  background: #fef2f2;
  box-shadow: none;
}

.semester-error-text {
  margin-top: 6px;
  white-space: pre-wrap;
}

.semester-error-actions {
  margin-top: 10px;
}

.semester-table-shell {
  margin-top: 0;
}

.semester-name {
  font-weight: 600;
}

.semester-dialog-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.semester-dialog-title {
  margin: 0;
  font-size: 16px;
}

.semester-form {
  padding: 6px 0 0;
}

.semester-form-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 10px 0;
}

.semester-form-label {
  min-width: 74px;
  color: #334155;
  font-weight: 600;
  font-size: 13px;
}

.semester-form-label.wide {
  min-width: 118px;
}

.semester-form-error {
  margin-top: 8px;
  color: #b91c1c;
  font-size: 12px;
  white-space: pre-wrap;
}

.semester-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.semester-config-tip {
  margin-bottom: 12px;
}

.semester-config-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 8px;
}

.semester-config-section {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 12px;
  background: #ffffff;
}

.semester-config-title {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.semester-config-sum {
  margin: 10px 0 0;
}

.semester-config-sum.invalid {
  color: #b91c1c;
  font-weight: 700;
}

@media (max-width: 768px) {
  .semester-config-grid {
    grid-template-columns: 1fr;
  }
}
</style>
