<template>
  <section class="card">
    <div class="toolbar">
      <div>
        <p class="muted" style="margin-top: 6px;">先选择待审核测评单，再逐项审核课程和活动。</p>
      </div>
    </div>

    <div class="table-search-bar">
      <div class="table-search-left">
        <SearchCapsule
          v-model="taskKeyword"
          width="180px"
          placeholder="搜索学号/姓名/班级"
          :disabled="loadingTasks"
          @submit="onTaskSearch"
          @clear="onTaskSearch"
        />
        <el-select v-model="taskStatusFilter" style="width: 132px;" :disabled="loadingTasks || loadingDetail || !!submittingTaskId">
          <el-option label="全部状态" value="ALL" />
          <el-option label="未审核" value="UNREVIEWED" />
          <el-option label="正在审核" value="IN_PROGRESS" />
          <el-option label="待复审" value="REVIEWED" />
          <el-option label="待提交" value="READY_TO_SUBMIT" />
          <el-option label="已提交" value="SUBMITTED" />
        </el-select>
      </div>
      <div class="table-search-right">
        <span class="muted">已选 {{ taskSelection.selectedCount.value }} 项</span>
        <el-button type="default" :disabled="!canBatchSubmitTasks" @click="batchSubmitTasksToAdmin">批量提交管理员</el-button>
      </div>
    </div>

    <div class="table-shell">
      <div class="table-scroll-main">
        <table class="table table-sticky table-fixed-right" style="--sticky-action-w: 156px; --sticky-status-w: 102px; --fixed-action-btn-w: 72px;" data-resize-key="teacher_tasks">
          <thead>
            <tr>
              <th style="width: 44px;">
                <input
                  ref="taskHeaderCheckboxRef"
                  type="checkbox"
                  :checked="taskAllCheckedOnPage"
                  :disabled="!pagedTaskIds.length || loadingTasks || loadingDetail || !!submittingTaskId"
                  @change="toggleAllTasksOnPage"
                />
              </th>
              <th>学号</th>
              <th>学生</th>
              <th>班级</th>
              <th>总分</th>
              <th>提交时间</th>
              <th class="col-status">状态</th>
              <th class="col-action">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="task in taskPager.pagedRows.value" :key="task.id">
              <td>
                <input
                  type="checkbox"
                  :checked="taskSelection.isSelected(task.id)"
                  :disabled="loadingTasks || loadingDetail || !!submittingTaskId"
                  @change="taskSelection.toggle(task.id)"
                />
              </td>
              <td>{{ task.account_no || task.accountNo || '-' }}</td>
              <td>{{ task.real_name }}</td>
              <td>
                <TableOverflowCell
                  :text="task.class_name"
                  :cell-key="`teacher_task_class_${task.id}`"
                />
              </td>
              <td>{{ task.total_score ?? '-' }}</td>
              <td>{{ formatDate(task.submitted_at) }}</td>
              <td class="col-status">
                <span class="badge" :class="taskProgressBadge(task)">{{ taskProgressLabel(task) }}</span>
                <span class="muted">{{ pickTaskDoneCount(task) }}/{{ pickTaskTotalCount(task) }}</span>
              </td>
              <td class="col-action">
                <div class="action-row inline-actions">
                  <el-button type="default" @click="openTask(task.id)" :disabled="loadingDetail || submittingTaskId === task.id">
                    {{ taskOpenActionLabel(task) }}
                  </el-button>
                  <el-button
                    type="primary"
                    @click="submitTaskToAdmin(task)"
                    :disabled="!canSubmitTask(task) || submittingTaskId === task.id"
                  >
                    {{ submitTaskButtonLabel(task) }}
                  </el-button>
                </div>
              </td>
            </tr>
            <tr v-if="!taskPager.pagedRows.value.length">
              <td colspan="8" class="empty">暂无符合条件的测评单</td>
            </tr>
          </tbody>
        </table>
      </div>
      <TablePager
        :page="taskPager.page.value"
        :total-pages="taskPager.totalPages.value"
        :total="taskPager.total.value"
        :page-size="taskPager.pageSize.value"
        :disabled="loadingTasks"
        @change="onTaskPageChange"
        @page-size-change="onTaskPageSizeChange"
      />
    </div>
  </section>

  <div v-if="drawerOpen && current" class="drawer-overlay" @click.self="closeDrawer">
    <div class="drawer-panel drawer-wide drawer-review">
      <div class="drawer-header">
        <div>
          <div style="font-weight: 700; font-size: 16px;">审核测评单 #{{ current.submission.id }}</div>
          <p class="muted" style="margin-top: 6px;">
            学号：<b>{{ current.student.accountNo || current.student.account_no || '-' }}</b>
            <span style="margin: 0 6px; color: #cbd5e1;">|</span>
            学生：<b>{{ current.student.realName }}</b>
            <span style="margin: 0 6px; color: #cbd5e1;">|</span>
            状态：<span class="badge">{{ current.submission.status }}</span>
          </p>
        </div>
        <el-button class="workspace-tool-btn" type="default" circle @click="closeDrawer" aria-label="关闭">×</el-button>
      </div>

      <div class="drawer-body">
        <h4 class="section-title">课程审核</h4>
        <div class="table-search-bar review-toolbar" style="margin-top: 0;">
          <div class="table-search-left">
            <SearchCapsule
              v-model="courseKeyword"
              width="180px"
              placeholder="搜索课程名称"
              :disabled="loadingDetail || isDeciding"
              @submit="onCourseSearch"
              @clear="onCourseSearch"
            />
            <el-select v-model="courseTypeFilter" style="width: 140px;" :disabled="loadingDetail || isDeciding">
              <el-option label="全部类型" value="ALL" />
              <el-option label="必修" value="REQUIRED" />
              <el-option label="选修" value="ELECTIVE" />
              <el-option label="重修" value="RETAKE" />
              <el-option label="再修" value="RELEARN" />
            </el-select>
            <el-select v-model="courseStatusFilter" style="width: 132px;" :disabled="loadingDetail || isDeciding">
              <el-option label="全部状态" value="ALL" />
              <el-option label="待审核" value="PENDING" />
              <el-option label="已通过" value="APPROVED" />
              <el-option label="已驳回" value="REJECTED" />
              <el-option label="待删除复审" value="DELETE_REQUESTED" />
              <el-option label="已删除" value="DELETED" />
            </el-select>
          </div>
          <div class="table-search-right review-toolbar-right">
            <span class="muted">已选 {{ courseSelection.selectedCount.value }} 项</span>
            <el-input
              class="review-batch-reason"
              v-model.trim="courseBatchRejectReason"
              placeholder="批量驳回理由（可空）"
              :disabled="isDeciding || loadingDetail || !canReviewCurrent"
            />
            <el-button class="review-batch-btn" type="default" :disabled="!canBatchCourseReview" @click="batchApproveCourses">批量通过</el-button>
            <el-button class="review-batch-btn" type="danger" :disabled="!canBatchCourseReview" @click="batchRejectCourses">批量驳回</el-button>
          </div>
        </div>

        <div class="table-shell">
          <div class="table-scroll-drawer">
            <table class="table table-sticky table-fixed-right" style="--sticky-action-w: 141px; --sticky-status-w: 102px; --fixed-action-btn-w: 66px; --fixed-action-gap: 3px; --fixed-action-padding-x: 3px;" data-resize-key="teacher_drawer_courses">
              <thead>
                <tr>
                  <th style="width: 44px;">
                    <input
                      ref="courseHeaderCheckboxRef"
                      type="checkbox"
                      :checked="courseAllCheckedOnPage"
                      :disabled="!pagedCourseIds.length || isDeciding || loadingDetail || !canReviewCurrent"
                      @change="toggleAllCoursesOnPage"
                    />
                  </th>
                  <th class="nowrap">课程</th>
                  <th class="nowrap">类型</th>
                  <th class="nowrap">原始分</th>
                  <th class="nowrap">证明材料</th>
                  <th>理由</th>
                  <th class="nowrap col-status">状态</th>
                  <th class="nowrap col-action">操作</th>
                </tr>
              </thead>
              <tbody>
            <tr v-for="course in coursePager.pagedRows.value" :key="`course_${course.id}`">
                  <td>
                    <input
                      type="checkbox"
                      :checked="courseSelection.isSelected(course.id)"
                      :disabled="isDeciding || loadingDetail || !canReviewCurrent"
                      @change="courseSelection.toggle(course.id)"
                    />
                  </td>
                  <td>
                    <TableOverflowCell
                      :text="course.courseName"
                      :cell-key="`teacher_drawer_course_name_${course.id}`"
                    />
                  </td>
                  <td class="nowrap">{{ courseTypeLabel(course.courseType) }}</td>
                  <td class="nowrap">{{ course.score }}</td>
                  <td>
                    <div v-if="course._evidenceMetas && course._evidenceMetas.length" class="chip-list">
                      <span v-for="m in course._evidenceMetas" :key="m.id" class="chip">
                        <button
                          class="link chip-file-link"
                          type="button"
                          :title="m.fileName || ('附件#' + m.id)"
                          @click="previewEvidence(m.id, course._evidenceMetas)"
                        >{{ m.fileName || ('附件#' + m.id) }}</button>
                      </span>
                    </div>
                    <span v-else class="muted" style="font-size: 12px;">未上传</span>
                  </td>
                  <td>
                    <el-input v-model.trim="drafts[courseKey(course.id)].reason" placeholder="可填写审核理由（选填）" :disabled="isDeciding || !canReviewCurrent" />
                  </td>
                  <td class="nowrap col-status">
                    <span class="badge" :class="reviewItemBadge(course)">
                      {{ reviewItemStatus(course) }}
                    </span>
                  </td>
                  <td class="col-action">
                    <div class="action-row inline-actions review-row-actions">
                      <template v-if="isDeleteRequested(course)">
                        <el-button type="primary" @click="decide('COURSE', course.id, 'APPROVE_DELETE')" :disabled="isDeciding || !canReviewCurrent">同意</el-button>
                        <el-button type="danger" @click="decide('COURSE', course.id, 'REJECT_DELETE')" :disabled="isDeciding || !canReviewCurrent">驳回</el-button>
                      </template>
                      <el-button v-else-if="isDeleted(course)" type="default" @click="decide('COURSE', course.id, 'UNDO_DELETE')" :disabled="isDeciding || !canReviewCurrent">撤回</el-button>
                      <template v-else-if="isPendingReviewable(course)">
                        <el-button type="primary" @click="decide('COURSE', course.id, 'APPROVE')" :disabled="isDeciding || !canReviewCurrent">通过</el-button>
                        <el-button type="danger" @click="decide('COURSE', course.id, 'REJECT')" :disabled="isDeciding || !canReviewCurrent">驳回</el-button>
                      </template>
                      <el-button v-else type="default" @click="decide('COURSE', course.id, 'UNDO')" :disabled="isDeciding || !canReviewCurrent">撤回</el-button>
                    </div>
                  </td>
                </tr>
                <tr v-if="!coursePager.pagedRows.value.length">
                  <td colspan="8" class="empty">暂无符合条件的课程</td>
                </tr>
              </tbody>
            </table>
          </div>
          <TablePager
            :page="coursePager.page.value"
            :total-pages="coursePager.totalPages.value"
            :total="coursePager.total.value"
            :page-size="coursePager.pageSize.value"
            :disabled="isDeciding || loadingDetail"
            @change="onCoursePageChange"
            @page-size-change="onCoursePageSizeChange"
          />
        </div>

        <h4 class="section-title">活动审核</h4>
        <div class="table-search-bar review-toolbar" style="margin-top: 0;">
          <div class="table-search-left">
            <SearchCapsule
              v-model="activityKeyword"
              width="180px"
              placeholder="搜索活动标题"
              :disabled="loadingDetail || isDeciding"
              @submit="onActivitySearch"
              @clear="onActivitySearch"
            />
            <el-select v-model="activityModuleFilter" style="width: 140px;" :disabled="loadingDetail || isDeciding">
              <el-option label="全部模块" value="ALL" />
              <el-option label="德育" value="MORAL" />
              <el-option label="智育" value="INTEL_PRO_INNOV" />
              <el-option label="体育" value="SPORT_ACTIVITY" />
              <el-option label="美育" value="ART" />
              <el-option label="劳育" value="LABOR" />
            </el-select>
            <el-select v-model="activityStatusFilter" style="width: 132px;" :disabled="loadingDetail || isDeciding">
              <el-option label="全部状态" value="ALL" />
              <el-option label="待审核" value="PENDING" />
              <el-option label="已通过" value="APPROVED" />
              <el-option label="已驳回" value="REJECTED" />
              <el-option label="待删除复审" value="DELETE_REQUESTED" />
              <el-option label="已删除" value="DELETED" />
            </el-select>
          </div>
          <div class="table-search-right review-toolbar-right">
            <span class="muted">已选 {{ activitySelection.selectedCount.value }} 项</span>
            <el-input
              class="review-batch-reason"
              v-model.trim="activityBatchRejectReason"
              placeholder="批量驳回理由（可空）"
              :disabled="isDeciding || loadingDetail || !canReviewCurrent"
            />
            <el-button class="review-batch-btn" type="default" :disabled="!canBatchActivityReview" @click="batchApproveActivities">批量通过</el-button>
            <el-button class="review-batch-btn" type="danger" :disabled="!canBatchActivityReview" @click="batchRejectActivities">批量驳回</el-button>
          </div>
        </div>

        <div class="table-shell">
          <div class="table-scroll-drawer">
            <table class="table table-sticky activity-table table-fixed-right" style="--sticky-action-w: 141px; --sticky-status-w: 102px; --fixed-action-btn-w: 66px; --fixed-action-gap: 3px; --fixed-action-padding-x: 3px;" data-resize-key="teacher_drawer_activities">
              <thead>
                <tr>
                  <th style="width: 44px;">
                    <input
                      ref="activityHeaderCheckboxRef"
                      type="checkbox"
                      :checked="activityAllCheckedOnPage"
                      :disabled="!pagedActivityIds.length || isDeciding || loadingDetail || !canReviewCurrent"
                      @change="toggleAllActivitiesOnPage"
                    />
                  </th>
                  <th class="nowrap col-module">模块</th>
                  <th class="nowrap col-title">标题</th>
                  <th class="nowrap col-score">原始分</th>
                  <th class="nowrap col-evidence">证明材料</th>
                  <th class="nowrap col-reason">理由</th>
                  <th class="nowrap col-status">状态</th>
                  <th class="nowrap col-action">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="activity in activityPager.pagedRows.value" :key="`act_${activity.id}`">
                  <td>
                    <input
                      type="checkbox"
                      :checked="activitySelection.isSelected(activity.id)"
                      :disabled="isDeciding || loadingDetail || !canReviewCurrent"
                      @change="activitySelection.toggle(activity.id)"
                    />
                  </td>
                  <td class="nowrap">{{ moduleLabel(activity.moduleType) }}</td>
                  <td>
                    <TableOverflowCell
                      :text="activity.title"
                      :cell-key="`teacher_drawer_activity_title_${activity.id}`"
                    />
                  </td>
                  <td class="nowrap">{{ activity.selfScore }}</td>
                  <td>
                    <div v-if="activity._evidenceMetas && activity._evidenceMetas.length" class="chip-list">
                      <span v-for="m in activity._evidenceMetas" :key="m.id" class="chip">
                        <button
                          class="link chip-file-link"
                          type="button"
                          :title="m.fileName || ('附件#' + m.id)"
                          @click="previewEvidence(m.id, activity._evidenceMetas)"
                        >{{ m.fileName || ('附件#' + m.id) }}</button>
                      </span>
                    </div>
                    <span v-else class="muted" style="font-size: 12px;">未上传</span>
                  </td>
                  <td>
                    <el-input v-model.trim="drafts[activityKey(activity.id)].reason" placeholder="可填写审核理由（选填）" :disabled="isDeciding || !canReviewCurrent" />
                  </td>
                  <td class="nowrap col-status">
                    <span class="badge" :class="reviewItemBadge(activity)">
                      {{ reviewItemStatus(activity) }}
                    </span>
                  </td>
                  <td class="col-action">
                    <div class="action-row inline-actions review-row-actions">
                      <template v-if="isDeleteRequested(activity)">
                        <el-button type="primary" @click="decide('ACTIVITY', activity.id, 'APPROVE_DELETE')" :disabled="isDeciding || !canReviewCurrent">同意</el-button>
                        <el-button type="danger" @click="decide('ACTIVITY', activity.id, 'REJECT_DELETE')" :disabled="isDeciding || !canReviewCurrent">驳回</el-button>
                      </template>
                      <el-button v-else-if="isDeleted(activity)" type="default" @click="decide('ACTIVITY', activity.id, 'UNDO_DELETE')" :disabled="isDeciding || !canReviewCurrent">撤回</el-button>
                      <template v-else-if="isPendingReviewable(activity)">
                        <el-button type="primary" @click="decide('ACTIVITY', activity.id, 'APPROVE')" :disabled="isDeciding || !canReviewCurrent">通过</el-button>
                        <el-button type="danger" @click="decide('ACTIVITY', activity.id, 'REJECT')" :disabled="isDeciding || !canReviewCurrent">驳回</el-button>
                      </template>
                      <el-button v-else type="default" @click="decide('ACTIVITY', activity.id, 'UNDO')" :disabled="isDeciding || !canReviewCurrent">撤回</el-button>
                    </div>
                  </td>
                </tr>
                <tr v-if="!activityPager.pagedRows.value.length">
                  <td colspan="8" class="empty">暂无符合条件的活动</td>
                </tr>
              </tbody>
            </table>
          </div>
          <TablePager
            :page="activityPager.page.value"
            :total-pages="activityPager.totalPages.value"
            :total="activityPager.total.value"
            :page-size="activityPager.pageSize.value"
            :disabled="isDeciding || loadingDetail"
            @change="onActivityPageChange"
            @page-size-change="onActivityPageSizeChange"
          />
        </div>
      </div>

      <div class="drawer-footer">
        <el-button type="default" @click="closeDrawer">关闭</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../api/http'
import { previewImageById } from '../utils/imagePreview'
import SearchCapsule from '../components/SearchCapsule.vue'
import TableOverflowCell from '../components/TableOverflowCell.vue'
import TablePager from '../components/TablePager.vue'
import useIdleAutoRefresh from '../composables/useIdleAutoRefresh'
import useTablePager from '../composables/useTablePager'
import useTableSelection from '../composables/useTableSelection'

const router = useRouter()
const route = useRoute()

const tasks = ref([])
const current = ref(null)
const selectedSubmissionId = ref(null)

const loadingTasks = ref(false)
const loadingDetail = ref(false)
const isDeciding = ref(false)
const submittingTaskId = ref(null)
const drawerOpen = ref(false)

const drafts = reactive({})
const evidenceMetaCache = reactive({})

const taskKeyword = ref('')
const taskStatusFilter = ref('ALL')
const courseKeyword = ref('')
const courseTypeFilter = ref('ALL')
const courseStatusFilter = ref('ALL')
const activityKeyword = ref('')
const activityModuleFilter = ref('ALL')
const activityStatusFilter = ref('ALL')
const courseBatchRejectReason = ref('')
const activityBatchRejectReason = ref('')

const courseKey = (id) => `COURSE_${id}`
const activityKey = (id) => `ACTIVITY_${id}`

const applyQueryPreset = () => {
  const presetRaw = route.query?.preset
  const preset = String(presetRaw || '').trim().toUpperCase()
  const allowed = new Set(['ALL', 'UNREVIEWED', 'IN_PROGRESS', 'REVIEWED', 'READY_TO_SUBMIT', 'SUBMITTED'])
  if (allowed.has(preset)) {
    taskStatusFilter.value = preset
  }
}

const consumeQuery = async () => {
  const next = { ...route.query }
  let changed = false
  if (next.open !== undefined) {
    delete next.open
    changed = true
  }
  if (next.preset !== undefined) {
    delete next.preset
    changed = true
  }
  if (changed) {
    await router.replace({ path: route.path, query: next })
  }
}

const filteredTasks = computed(() => {
  const kw = String(taskKeyword.value || '').trim().toLowerCase()
  return tasks.value.filter((task) => {
    const source = `${task.account_no || task.accountNo || ''} ${task.real_name || ''} ${task.class_name || ''}`.toLowerCase()
    const matchKeyword = !kw || source.includes(kw)
    const matchStatus = taskStatusFilter.value === 'ALL' || taskProgressCode(task) === taskStatusFilter.value
    return matchKeyword && matchStatus
  })
})

const taskPager = useTablePager(filteredTasks, 10)
const canReviewCurrent = computed(() => String(current.value?.submission?.status || '').trim().toUpperCase() === 'SUBMITTED')

const filteredCourses = computed(() => {
  const list = current.value?.courses || []
  const kw = String(courseKeyword.value || '').trim().toLowerCase()
  return list.filter((item) => {
    const matchKeyword = !kw || String(item.courseName || '').toLowerCase().includes(kw)
    const matchType = courseTypeFilter.value === 'ALL' || String(item.courseType || '').toUpperCase() === courseTypeFilter.value
    const matchStatus = courseStatusFilter.value === 'ALL' || reviewItemStatusCode(item) === courseStatusFilter.value
    return matchKeyword && matchType && matchStatus
  })
})

const coursePager = useTablePager(filteredCourses, 10)

const filteredActivities = computed(() => {
  const list = current.value?.activities || []
  const kw = String(activityKeyword.value || '').trim().toLowerCase()
  return list.filter((item) => {
    const matchKeyword = !kw || String(item.title || '').toLowerCase().includes(kw)
    const matchModule = activityModuleFilter.value === 'ALL' || String(item.moduleType || '').toUpperCase() === activityModuleFilter.value
    const matchStatus = activityStatusFilter.value === 'ALL' || reviewItemStatusCode(item) === activityStatusFilter.value
    return matchKeyword && matchModule && matchStatus
  })
})

const activityPager = useTablePager(filteredActivities, 10)
const taskSelection = useTableSelection()
const courseSelection = useTableSelection()
const activitySelection = useTableSelection()

const taskHeaderCheckboxRef = ref(null)
const courseHeaderCheckboxRef = ref(null)
const activityHeaderCheckboxRef = ref(null)

const pagedTaskIds = computed(() => taskPager.pagedRows.value.map((item) => item.id))
const pagedCourseIds = computed(() => coursePager.pagedRows.value.map((item) => item.id))
const pagedActivityIds = computed(() => activityPager.pagedRows.value.map((item) => item.id))

const taskAllCheckedOnPage = computed(() => taskSelection.isAllCheckedOnPage(pagedTaskIds.value))
const courseAllCheckedOnPage = computed(() => courseSelection.isAllCheckedOnPage(pagedCourseIds.value))
const activityAllCheckedOnPage = computed(() => activitySelection.isAllCheckedOnPage(pagedActivityIds.value))

const taskIndeterminateOnPage = computed(() => taskSelection.isIndeterminateOnPage(pagedTaskIds.value))
const courseIndeterminateOnPage = computed(() => courseSelection.isIndeterminateOnPage(pagedCourseIds.value))
const activityIndeterminateOnPage = computed(() => activitySelection.isIndeterminateOnPage(pagedActivityIds.value))

const canBatchSubmitTasks = computed(() => (
  taskSelection.selectedCount.value > 0
  && !loadingTasks.value
  && !loadingDetail.value
  && !isDeciding.value
  && !submittingTaskId.value
))

const canBatchCourseReview = computed(() => (
  courseSelection.selectedCount.value > 0
  && canReviewCurrent.value
  && !loadingDetail.value
  && !isDeciding.value
))

const canBatchActivityReview = computed(() => (
  activitySelection.selectedCount.value > 0
  && canReviewCurrent.value
  && !loadingDetail.value
  && !isDeciding.value
))

const courseTypeLabel = (raw) => {
  const code = (raw || '').trim().toUpperCase()
  if (code === 'REQUIRED') return '必修'
  if (code === 'ELECTIVE') return '选修'
  if (code === 'RETAKE') return '重修'
  if (code === 'RELEARN') return '再修'
  return raw || '-'
}

const moduleLabel = (raw) => {
  const code = (raw || '').trim().toUpperCase()
  if (code === 'MORAL') return '德育'
  if (code === 'INTEL_PRO_INNOV') return '智育'
  if (code === 'SPORT_ACTIVITY') return '体育'
  if (code === 'ART') return '美育'
  if (code === 'LABOR') return '劳育'
  return raw || '-'
}

const isPendingStatus = (raw) => String(raw || '').trim().toUpperCase() === 'PENDING'
const normalizeDeleteState = (raw) => {
  const state = String(raw || '').trim().toUpperCase()
  return state || 'NONE'
}
const isDeleteRequested = (item) => normalizeDeleteState(item?.deleteState || item?.delete_state) === 'DELETE_REQUESTED'
const isDeleted = (item) => normalizeDeleteState(item?.deleteState || item?.delete_state) === 'DELETED'
const isPendingReviewable = (item) => isPendingStatus(item?.reviewStatus) && !isDeleteRequested(item) && !isDeleted(item)

const reviewItemStatusCode = (item) => {
  if (isDeleteRequested(item)) return 'DELETE_REQUESTED'
  if (isDeleted(item)) return 'DELETED'
  const reviewStatus = String(item?.reviewStatus || '').trim().toUpperCase()
  if (reviewStatus === 'APPROVED') return 'APPROVED'
  if (reviewStatus === 'REJECTED') return 'REJECTED'
  return 'PENDING'
}

const reviewItemStatus = (item) => {
  const code = reviewItemStatusCode(item)
  if (code === 'DELETE_REQUESTED') return '待删除复审'
  if (code === 'DELETED') return '已删除'
  if (code === 'APPROVED') return '已通过'
  if (code === 'REJECTED') return '已驳回'
  return '待审核'
}

const reviewItemBadge = (item) => {
  const label = reviewItemStatus(item)
  if (label === '已通过') return 'success'
  if (label === '已驳回') return 'danger'
  if (label === '待删除复审') return 'warning'
  return ''
}

const pickTaskDoneCount = (task) => {
  const raw = task?.reviewDoneCount ?? task?.review_done_count ?? 0
  const value = Number(raw)
  return Number.isFinite(value) ? value : 0
}

const pickTaskTotalCount = (task) => {
  const raw = task?.reviewTotalCount ?? task?.review_total_count ?? 0
  const value = Number(raw)
  return Number.isFinite(value) ? value : 0
}

const pickTaskRejectedCount = (task) => {
  const raw = task?.reviewRejectedCount ?? task?.review_rejected_count ?? 0
  const value = Number(raw)
  return Number.isFinite(value) ? value : 0
}

const pickTaskPhase = (task) => {
  const raw = String(task?.reviewPhase || task?.review_phase || task?.review_progress || '').trim().toUpperCase()
  if (raw === 'DONE') {
    return pickTaskRejectedCount(task) > 0 ? 'REVIEWED' : 'READY_TO_SUBMIT'
  }
  return raw
}

const isSubmittedToAdminTask = (task) => String(task?.status || '').trim().toUpperCase() === 'COUNSELOR_REVIEWED'

const taskProgressCode = (task) => {
  if (isSubmittedToAdminTask(task)) return 'SUBMITTED'
  const phase = pickTaskPhase(task)
  if (phase === 'READY_TO_SUBMIT') return 'READY_TO_SUBMIT'
  if (phase === 'REVIEWED') return 'REVIEWED'
  if (phase === 'IN_PROGRESS') return 'IN_PROGRESS'
  return 'UNREVIEWED'
}

const taskProgressLabel = (task) => {
  const code = taskProgressCode(task)
  if (code === 'SUBMITTED') return '已提交'
  if (code === 'READY_TO_SUBMIT') return '待提交'
  if (code === 'REVIEWED') return '待复审'
  if (code === 'IN_PROGRESS') return '正在审核'
  return '未审核'
}

const taskProgressBadge = (task) => {
  const code = taskProgressCode(task)
  if (code === 'SUBMITTED' || code === 'READY_TO_SUBMIT') return 'success'
  if (code === 'REVIEWED') return 'warning'
  if (code === 'IN_PROGRESS') return ''
  return 'danger'
}

const canSubmitTask = (task) => {
  if (!task) return false
  if (isSubmittedToAdminTask(task)) return false
  const progress = pickTaskPhase(task)
  const status = String(task.status || '').trim().toUpperCase()
  return progress === 'READY_TO_SUBMIT' && status === 'SUBMITTED'
}

const taskOpenActionLabel = (task) => {
  const opened = selectedSubmissionId.value === task?.id
  if (isSubmittedToAdminTask(task)) {
    return opened ? '已查看' : '查看'
  }
  return opened ? '已打开' : '打开审核'
}

const submitTaskButtonLabel = (task) => {
  if (submittingTaskId.value === task?.id) return '提交中...'
  if (isSubmittedToAdminTask(task)) return '已提交'
  return '提交'
}

const isAutoReason = (text) => {
  const value = String(text || '').trim().toUpperCase()
  return value === '辅导员APPROVE'.toUpperCase() || value === '辅导员REJECT'.toUpperCase()
}

const normalizeReason = (text) => {
  if (!text || isAutoReason(text)) return ''
  return String(text).trim()
}

const ensureDraft = (key) => {
  if (!drafts[key]) {
    drafts[key] = {
      reason: ''
    }
  }
}

const initDrafts = (detail) => {
  ;(detail.courses || []).forEach((course) => {
    const key = courseKey(course.id)
    ensureDraft(key)
    drafts[key].reason = normalizeReason(course.reviewerComment)
  })

  ;(detail.activities || []).forEach((activity) => {
    const key = activityKey(activity.id)
    ensureDraft(key)
    drafts[key].reason = normalizeReason(activity.reviewerComment)
  })
}

const loadTasks = async ({ keepPage = false } = {}) => {
  loadingTasks.value = true
  try {
    const { data } = await http.get('/reviews/tasks')
    tasks.value = data.data || []
    taskSelection.clear()
    if (!keepPage) {
      taskPager.resetPage()
    }
  } finally {
    loadingTasks.value = false
  }
}

const resetDrawerFilters = () => {
  courseKeyword.value = ''
  courseTypeFilter.value = 'ALL'
  courseStatusFilter.value = 'ALL'
  activityKeyword.value = ''
  activityModuleFilter.value = 'ALL'
  activityStatusFilter.value = 'ALL'
  courseBatchRejectReason.value = ''
  activityBatchRejectReason.value = ''
  coursePager.resetPage()
  activityPager.resetPage()
  courseSelection.clear()
  activitySelection.clear()
}

const openTask = async (submissionId) => {
  loadingDetail.value = true
  try {
    const { data } = await http.get(`/submissions/${submissionId}`)
    current.value = data.data
    selectedSubmissionId.value = submissionId
    initDrafts(data.data)
    await hydrateEvidenceMetas()
    resetDrawerFilters()
    courseSelection.clear()
    activitySelection.clear()
    drawerOpen.value = true
  } finally {
    loadingDetail.value = false
  }
}

const closeDrawer = () => {
  drawerOpen.value = false
  courseSelection.clear()
  activitySelection.clear()
}

const reloadCurrent = async () => {
  if (!selectedSubmissionId.value) return
  await openTask(selectedSubmissionId.value)
}

const decide = async (itemType, itemId, action) => {
  if (!canReviewCurrent.value) {
    alert('该测评单当前不可继续审核，请刷新列表')
    return
  }
  const key = itemType === 'COURSE' ? courseKey(itemId) : activityKey(itemId)
  ensureDraft(key)

  const payload = {
    action,
    reason: (drafts[key].reason || '').trim()
  }

  isDeciding.value = true
  try {
    await http.post(`/reviews/items/${itemType}/${itemId}/decision`, payload)
    await reloadCurrent()
    await loadTasks({ keepPage: true })
    alert('审核操作已提交')
  } finally {
    isDeciding.value = false
  }
}

const submitTaskToAdmin = async (task) => {
  if (!task?.id) return
  if (!canSubmitTask(task)) {
    alert('仅“待提交”状态可提交管理员（需全条目已通过）')
    return
  }

  submittingTaskId.value = task.id
  try {
    await http.post(`/reviews/submissions/${task.id}/submit`)
    if (selectedSubmissionId.value === task.id) {
      closeDrawer()
      current.value = null
      selectedSubmissionId.value = null
    }
    await loadTasks({ keepPage: true })
    alert('已提交管理员')
  } finally {
    submittingTaskId.value = null
  }
}

const formatDate = (raw) => {
  if (!raw) return '-'
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) return raw
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
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

const hydrateEvidenceMetas = async () => {
  if (!current.value) return

  const ids = []
  for (const c of (current.value.courses || [])) {
    const courseEvidenceId = Number(c?.evidenceFileId)
    if (Number.isFinite(courseEvidenceId) && courseEvidenceId > 0) {
      ids.push(courseEvidenceId)
    }
  }
  for (const a of (current.value.activities || [])) {
    ids.push(...parseEvidenceIds(a.evidenceFileIds))
  }
  const uniqueIds = Array.from(new Set(ids))
  if (!uniqueIds.length) {
    for (const c of (current.value.courses || [])) {
      c._evidenceMetas = []
    }
    for (const a of (current.value.activities || [])) {
      a._evidenceMetas = []
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

  for (const c of (current.value.courses || [])) {
    const courseEvidenceId = Number(c?.evidenceFileId)
    if (!Number.isFinite(courseEvidenceId) || courseEvidenceId <= 0) {
      c._evidenceMetas = []
      continue
    }
    c._evidenceMetas = [map[courseEvidenceId] || evidenceMetaCache[courseEvidenceId] || { id: courseEvidenceId, fileName: `附件#${courseEvidenceId}` }]
  }

  for (const a of (current.value.activities || [])) {
    const aIds = parseEvidenceIds(a.evidenceFileIds)
    a._evidenceMetas = aIds.map((id) => map[id] || evidenceMetaCache[id] || { id, fileName: `附件#${id}` })
  }
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

const selectedTasksOnPage = computed(() => {
  const selected = new Set(taskSelection.selectedList.value)
  return taskPager.pagedRows.value.filter((item) => selected.has(String(item.id)))
})

const selectedCoursesOnPage = computed(() => {
  const selected = new Set(courseSelection.selectedList.value)
  return coursePager.pagedRows.value.filter((item) => selected.has(String(item.id)))
})

const selectedActivitiesOnPage = computed(() => {
  const selected = new Set(activitySelection.selectedList.value)
  return activityPager.pagedRows.value.filter((item) => selected.has(String(item.id)))
})

const toggleAllTasksOnPage = () => {
  taskSelection.toggleAll(pagedTaskIds.value)
}

const toggleAllCoursesOnPage = () => {
  courseSelection.toggleAll(pagedCourseIds.value)
}

const toggleAllActivitiesOnPage = () => {
  activitySelection.toggleAll(pagedActivityIds.value)
}

const onTaskPageChange = (nextPage) => {
  taskPager.goPage(nextPage)
  taskSelection.clear()
}

const onTaskPageSizeChange = (nextSize) => {
  taskPager.setPageSize(nextSize)
  taskSelection.clear()
}

const onCoursePageChange = (nextPage) => {
  coursePager.goPage(nextPage)
  courseSelection.clear()
}

const onCoursePageSizeChange = (nextSize) => {
  coursePager.setPageSize(nextSize)
  courseSelection.clear()
}

const onActivityPageChange = (nextPage) => {
  activityPager.goPage(nextPage)
  activitySelection.clear()
}

const onActivityPageSizeChange = (nextSize) => {
  activityPager.setPageSize(nextSize)
  activitySelection.clear()
}

const batchSubmitTasksToAdmin = async () => {
  const selectedRows = selectedTasksOnPage.value
  if (!selectedRows.length) {
    alert('请先勾选要处理的数据')
    return
  }
  if (!confirm(`确认批量提交管理员已选 ${selectedRows.length} 项吗？`)) return

  let success = 0
  let failed = 0
  let skipped = 0
  let shouldCloseDrawer = false

  for (const task of selectedRows) {
    if (!canSubmitTask(task)) {
      skipped += 1
      continue
    }
    try {
      await http.post(`/reviews/submissions/${task.id}/submit`)
      if (selectedSubmissionId.value === task.id) shouldCloseDrawer = true
      success += 1
    } catch (e) {
      failed += 1
    }
  }

  if (shouldCloseDrawer) {
    closeDrawer()
    current.value = null
    selectedSubmissionId.value = null
  }
  taskSelection.clear()
  await loadTasks({ keepPage: true })
  alert(`批量提交完成：成功 ${success}，失败 ${failed}，跳过 ${skipped}`)
}

const runBatchDecision = async ({ itemType, action, reason }) => {
  const selectedRows = itemType === 'COURSE' ? selectedCoursesOnPage.value : selectedActivitiesOnPage.value
  if (!selectedRows.length) {
    alert('请先勾选要处理的数据')
    return
  }

  const actionLabel = action === 'APPROVE' ? '批量通过' : '批量驳回'
  if (!confirm(`确认${actionLabel}已选 ${selectedRows.length} 项吗？`)) return

  isDeciding.value = true
  try {
    let success = 0
    let failed = 0
    let skipped = 0

    for (const row of selectedRows) {
      if (!isPendingReviewable(row)) {
        skipped += 1
        continue
      }
      try {
        await http.post(`/reviews/items/${itemType}/${row.id}/decision`, {
          action,
          reason: (reason || '').trim()
        })
        success += 1
      } catch (e) {
        failed += 1
      }
    }

    courseSelection.clear()
    activitySelection.clear()
    await reloadCurrent()
    await loadTasks({ keepPage: true })
    alert(`${actionLabel}完成：成功 ${success}，失败 ${failed}，跳过 ${skipped}`)
  } finally {
    isDeciding.value = false
  }
}

const batchApproveCourses = async () => {
  await runBatchDecision({ itemType: 'COURSE', action: 'APPROVE', reason: '' })
}

const batchRejectCourses = async () => {
  await runBatchDecision({
    itemType: 'COURSE',
    action: 'REJECT',
    reason: courseBatchRejectReason.value
  })
}

const batchApproveActivities = async () => {
  await runBatchDecision({ itemType: 'ACTIVITY', action: 'APPROVE', reason: '' })
}

const batchRejectActivities = async () => {
  await runBatchDecision({
    itemType: 'ACTIVITY',
    action: 'REJECT',
    reason: activityBatchRejectReason.value
  })
}

const onTaskSearch = () => {
  taskSelection.clear()
  taskPager.resetPage()
}

const onCourseSearch = () => {
  courseSelection.clear()
  coursePager.resetPage()
}

const onActivitySearch = () => {
  activitySelection.clear()
  activityPager.resetPage()
}

const resetTaskFilter = () => {
  taskKeyword.value = ''
  taskStatusFilter.value = 'ALL'
  taskSelection.clear()
  taskPager.resetPage()
}

const resetCourseFilter = () => {
  courseKeyword.value = ''
  courseTypeFilter.value = 'ALL'
  courseStatusFilter.value = 'ALL'
  courseSelection.clear()
  coursePager.resetPage()
}

const resetActivityFilter = () => {
  activityKeyword.value = ''
  activityModuleFilter.value = 'ALL'
  activityStatusFilter.value = 'ALL'
  activitySelection.clear()
  activityPager.resetPage()
}

watch(taskStatusFilter, () => {
  taskSelection.clear()
  taskPager.resetPage()
})

watch([courseKeyword, courseTypeFilter, courseStatusFilter], () => {
  courseSelection.clear()
  coursePager.resetPage()
})

watch([activityKeyword, activityModuleFilter, activityStatusFilter], () => {
  activitySelection.clear()
  activityPager.resetPage()
})

watch(taskIndeterminateOnPage, (value) => {
  if (!taskHeaderCheckboxRef.value) return
  taskHeaderCheckboxRef.value.indeterminate = value
})

watch(courseIndeterminateOnPage, (value) => {
  if (!courseHeaderCheckboxRef.value) return
  courseHeaderCheckboxRef.value.indeterminate = value
})

watch(activityIndeterminateOnPage, (value) => {
  if (!activityHeaderCheckboxRef.value) return
  activityHeaderCheckboxRef.value.indeterminate = value
})

const busyRef = computed(() => loadingTasks.value || loadingDetail.value || isDeciding.value || !!submittingTaskId.value)
const pausedRef = computed(() => drawerOpen.value)

useIdleAutoRefresh({
  refreshFn: () => loadTasks({ keepPage: true }),
  intervalMs: 30000,
  isBusy: busyRef,
  isPaused: pausedRef
})

onMounted(async () => {
  applyQueryPreset()
  await loadTasks()

  const openRaw = route.query?.open
  const openId = Number(openRaw)
  if (Number.isFinite(openId) && openId > 0) {
    await openTask(openId)
  }

  await consumeQuery()
})
</script>

<style scoped>
.nowrap {
  white-space: nowrap;
}

.inline-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: nowrap;
}

.review-toolbar {
  gap: 6px;
}

.review-toolbar .table-search-left,
.review-toolbar .table-search-right {
  gap: 6px;
}

.review-toolbar-right :deep(.review-batch-reason.el-input) {
  width: 178px;
}

.review-toolbar-right :deep(.review-batch-reason .el-input__wrapper) {
  min-height: 28px;
  padding-top: 0;
  padding-bottom: 0;
}

.review-toolbar-right :deep(.review-batch-btn.el-button) {
  height: 28px;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 9px;
  font-size: 12px;
  font-weight: 500;
}

.drawer-review :deep(.table-fixed-right td.col-action .review-row-actions) {
  gap: 2px;
}

.drawer-review :deep(.table-fixed-right td.col-action .review-row-actions .el-button) {
  height: 24px;
  min-height: 24px;
  width: 64px;
  min-width: 64px;
  max-width: 64px;
  padding: 0 6px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 500;
}

.activity-table .col-module,
.activity-table .col-title,
.activity-table .col-score,
.activity-table .col-evidence,
.activity-table .col-reason,
.activity-table .col-action {
  white-space: nowrap;
}
</style>

