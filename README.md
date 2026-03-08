# 大学生综合测评管理系统

这是一个前后端分离的综合测评项目：
- `frontend/`：Vue 3 + Vite + Element Plus 前端
- `backend/`：Spring Boot + MyBatis 后端
- `scripts/`：根目录联调/测试脚本

## 仓库结构

### 需要重点关注的源码目录
- `frontend/src/`
  - `views/`：页面视图
  - `components/`：通用组件
  - `layouts/`：布局壳层
  - `styles/`：全局样式和页面基线
  - `utils/`：前端工具函数
- `backend/src/main/java/com/cqnu/eval/`
  - `controller/`：接口入口
  - `service/`：业务逻辑
  - `mapper/`：数据库访问
  - `model/`：DTO 和实体
  - `security/`：鉴权与 JWT
- `backend/src/main/resources/`
  - `db/`：建表、种子数据、迁移脚本
  - `report-templates/`：报表导出模板

### 有明确用途、建议保留的目录
- `scripts/`：从仓库根目录发起的完整测试脚本
- `backend/scripts/`：数据库初始化、后端启动脚本
- `.vscode/`：当前仓库的编辑器建议配置
- `SESSION_NOTES.md`：本地会话记录，后续可迁移到 `docs/` 体系

## 当前仓库里容易混淆的目录或文件

### 产物 / 运行文件
这些不是核心源码，通常不应该进入版本库：
- `frontend/node_modules/`
- `frontend/dist/`
- `frontend/playwright-report/`
- `frontend/test-results/`
- `test-results/`
- `backend/target/`
- `backend/*.log`
- `frontend/*.log`

这些路径已在根目录 `.gitignore` 中补充，后续新生成文件会被忽略。

### 需要人工确认后再删的历史文件
这些更像调试或实验残留，但本次没有直接删除：
- `backend/export_check.docx`
- `backend/export_check.zip`
- `backend/export_pretty.xml`
- `backend/customXml/`
- `backend/export_check_unzip2/`
- `backend/tmp_template_xml/`
- 根目录 `uploads/`

说明：
- `backend/src/main/resources/application.yml` 当前上传目录配置是 `./uploads`，运行时实际更接近 `backend/uploads/`。
- 根目录 `uploads/` 更像历史残留目录，不是当前主运行路径。
- `backend/uploads/` 里目前有已跟踪的图片文件，可能被种子数据或演示流程引用，删除前需要单独确认。

## 当前整理结论
- 真正需要长期维护的核心内容，主要集中在：`frontend/src/`、`backend/src/`、`backend/src/main/resources/`。
- 当前“文件太多”的主要原因不是源码过多，而是构建产物、依赖、日志、测试报告混在仓库里。
- 仅补 `.gitignore` 还不会自动把已被 Git 跟踪的产物移出版本库；如果后续要彻底精简，需要再做一次“取消跟踪”清理。

## 建议的下一步
如果要继续做低风险整理，建议按这个顺序：
1. 先确认 `backend/uploads/` 是否属于必须保留的演示样本数据。
2. 再确认 `backend/export_check*`、`customXml/`、`export_check_unzip2/` 是否还服务于当前导出逻辑。
3. 最后再做一次“已跟踪产物清理”，把 `dist/`、`target/`、日志文件从 Git 中移除。
