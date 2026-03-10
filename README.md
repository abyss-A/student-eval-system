# 大学生综合测评管理系统

## 项目简介
这是一个面向高校场景的“大学生综合测评管理系统”，采用前后端分离架构，覆盖学生填报、辅导员审核、管理员只读查看测评单、公告通知、反馈处理、综合排名、班级权限管理等核心流程。

## 技术栈
### 前端
- Vue 3
- Vite
- Element Plus
- Vue Router
- Playwright（端到端测试）

### 后端
- Spring Boot 2.7.x
- MyBatis
- MySQL
- JWT 鉴权
- Apache POI / OpenPDF（报表导出）

## 功能概览
### 学生端
- 账号由管理员统一创建，登录后可自行修改密码
- 填报课程成绩
- 填报德智体美劳活动
- 查看预览分数与审核分数
- 提交审核 / 再次提交
- 导出正式 Word（当前规则：辅导员提交管理员后才允许）
- 查看公告、反馈、排名、账号资料

### 辅导员端
- 账号由管理员统一创建，登录后可自行修改密码
- 查看待审核测评单
- 审核课程和活动条目
- 发起通过 / 驳回 / 删除复审
- 整单审核后提交管理员
- 查看公告、反馈、排名、账号资料

### 管理员端
- 查看辅导员已提交的测评单
- 查看课程 / 活动审核明细
- 管理学生与辅导员账号（单个创建、批量导入、启停、重置密码、受限删除）
- 管理班级权限
- 管理公告与反馈
- 查看综合排名与账号资料

## 目录结构
- `frontend/`：前端工程
- `frontend/src/`：前端核心源码
- `frontend/tests/`：前端 Playwright 测试
- `backend/`：后端工程
- `backend/src/main/java/com/cqnu/eval/`：后端业务代码
- `backend/src/main/resources/`：配置、SQL、Mapper、报表模板
- `backend/scripts/`：数据库初始化与后端启动脚本
- `scripts/`：整仓联调 / 测试脚本
- `docs/`：项目上下文、交接、UI 与部署文档

## 快速启动
### 前端
```powershell
cd frontend
npm install
npm run dev -- --host 0.0.0.0 --port 5173
```

### 后端
```powershell
cd backend
powershell -ExecutionPolicy Bypass -File .\scripts\run-backend.ps1
```

### 数据库初始化
```powershell
cd backend
powershell -ExecutionPolicy Bypass -File .\scripts\init-db.ps1
```

## 本地默认运行约定
- 前端：`http://localhost:5173`
- 后端：`http://localhost:8080`
- 数据库：`student_eval`
- 数据库账号：`root / 123456`
- 上传目录：由 `app.upload-dir` 决定，当前默认 `./uploads`

## 仓库维护约定
- 构建产物、日志、测试报告、依赖目录不提交到仓库
- 代码与配置文件统一使用 `UTF-8`
- Markdown / 文本说明文档使用 `UTF-8 with BOM`
- 提交信息优先使用中文

## 文档入口
- `docs/project-context.md`：项目背景、模块、角色、状态与业务口径
- `docs/current-handoff.md`：当前进展、已完成改动、风险点、下一步建议
- `docs/ui-reference.md`：UI 风格、交互和术语统一口径
- `docs/deployment-notes.md`：本地开发与学校服务器部署说明
- `backend/README.md`：后端专项说明

## 当前整理状态
- 已清理已跟踪的构建产物、日志、测试报告和历史调试残留文件
- 已补 `.gitignore`、`.editorconfig` 和 `docs/` 文档骨架
- 后续新会话或新开发者应先阅读本 README 与 `docs/` 下文档
