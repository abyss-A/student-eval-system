# 项目背景与结构

## 1. 项目定位
这是一个面向高校场景的“大学生综合测评管理系统”。

项目目标不是做通用后台，而是围绕学生综合测评的真实业务流程提供：
- 学生填报课程成绩、德智体美劳活动成绩、提交测评单
- 辅导员审核课程与活动明细、决定是否提交管理员
- 管理员查看辅导员已提交测评单，以及账号、公告、反馈、排名等后台结果
- 公告通知、反馈处理、综合排名、账号中心、班级权限管理

## 2. 技术栈
### 前端
- Vue 3
- Vite
- Element Plus
- Vue Router
- Playwright

### 后端
- Spring Boot 2.7.x
- MyBatis
- MySQL
- JWT 鉴权
- Apache POI / OpenPDF（报表导出）

## 3. 主要角色与边界
### 学生
- 账号由管理员统一创建
- 编辑自己的测评单
- 填报课程成绩、活动成绩、证明材料
- 提交审核、查看审核结果、再次提交
- 导出正式 Word（有状态门槛）

### 辅导员
- 账号由管理员统一创建
- 只能审核自己有班级范围权限的学生
- 审核课程、活动、删除复审
- 整单确认无误后提交管理员

### 管理员
- 查看辅导员已提交管理员的测评单
- 对测评单仅做只读查看，不承担终审状态推进职责
- 不进入辅导员审核流
- 管理班级权限、账号、公告、反馈等后台能力

## 4. 核心模块
- 登录 / 管理员建号 / JWT 认证
- 学生测评填报
- 辅导员审核流
- 管理员只读查看流
- 账号管理
- 排名
- 公告通知
- 反馈管理
- 附件上传与预览
- Word 报表导出

## 5. 路由结构摘要
### 学生端
- `/student/eval/course`
- `/student/eval/moral`
- `/student/eval/intel`
- `/student/eval/sport`
- `/student/eval/art`
- `/student/eval/labor`
- `/student/eval/submit`
- `/student/notices`
- `/student/feedback/*`
- `/student/ranking`
- `/student/me/profile`

### 辅导员端
- `/teacher/review/tasks`
- `/teacher/notices`
- `/teacher/feedback/*`
- `/teacher/ranking`
- `/teacher/me/profile`

### 管理员端
- `/admin/submissions`
- `/admin/counselor/scopes`
- `/admin/accounts`
- `/admin/notices`
- `/admin/feedback/handle`
- `/admin/ranking`
- `/admin/me/profile`

## 6. 状态机口径
### 提交单状态
- `DRAFT`：草稿，可自由编辑
- `SUBMITTED`：学生已提交，辅导员审核中
- `COUNSELOR_REVIEWED`：辅导员已提交管理员，管理员可只读查看
- `FINALIZED`：历史兼容状态；当前正式业务不要求管理员终审测评单
- `PUBLISHED`：历史兼容状态；当前正式业务不要求管理员推进测评单公示状态

### 审核阶段状态
- `NOT_REVIEWED`
- `IN_PROGRESS`
- `DONE_NEED_STUDENT_FIX`
- `DONE_ALL_PASS`

### 删除状态
- `NONE`
- `DELETE_PENDING_SUBMIT`
- `DELETE_REQUESTED`
- `DELETED`

## 7. 分数口径
### 原始 / 预览口径
- 学生原始填写内容
- 用于学生编辑阶段展示与预览计算

### 审核 / 生效口径
- 辅导员审核后的有效结果
- 管理员主表总分按这个口径展示
- 正式导出 Word 也按正式结果理解

## 8. 文件与数据存放口径
### 代码与配置
- `frontend/src/`
- `backend/src/main/java/`
- `backend/src/main/resources/`

### 运行数据
- `backend/uploads/`（由 `app.upload-dir` 控制）
- 数据库中的附件记录只存元数据与路径，不存图片二进制本体

### 构建产物
- `frontend/dist/`
- `frontend/node_modules/`
- `backend/target/`
- 各类日志、测试报告

## 9. 当前已确认的关键业务规则
- 学生正式导出 Word：辅导员提交管理员后才开放
- 学生与辅导员账号由管理员统一创建，自助注册不再作为正式业务流程
- 管理员看的是评审生效结果，不是学生原始预览值
- 被驳回条目删除：先标记“待提交删除”，再次提交时才真正送老师复审
- UI 风格以紧凑、清爽、统一术语为原则

## 10. 学期口径
- 系统以 `semester` 作为评测周期：每个学期每个学生最多一份测评单（`submission` 表 `semester_id + student_id` 唯一）。
- “当前学期”由管理员在后台“学期管理”维护；学生进入填报时，会基于当前学期自动创建或获取测评单。
- 切换当前学期前，需确保当前学期不存在 `SUBMITTED`（待辅导员审核）的测评单，否则系统将阻止切换并提示数量。
