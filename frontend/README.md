# 前端专项说明

## 1. 前端职责
前端负责：
- 学生填报与分数预览
- 辅导员审核抽屉与批量操作
- 管理员查看与只读抽屉
- 公告、反馈、排名、账号中心
- 正式 Word 导出入口与状态提示

## 2. 关键目录
- `src/views/`：业务页面
- `src/components/`：通用组件
- `src/layouts/`：布局与导航
- `src/styles/`：全局样式和布局基线
- `src/utils/`：认证、图片预览、表格列宽等工具
- `tests/`：Playwright 用例

## 3. 本地运行
```powershell
cd frontend
npm install
npm run dev -- --host 0.0.0.0 --port 5173
```

## 4. 构建
```powershell
cd frontend
npm run build
```

## 5. 当前重要前端口径
- 搜索框偏窄，避免工具栏过松
- 表格右侧状态列 / 操作列在桌面端优先固定
- 文案、表头、按钮尽量统一术语
- 学生正式导出 Word 仅在辅导员提交管理员后开放
- 删除被驳回条目时先进入“待提交删除”，再次提交后才进入老师删除复审