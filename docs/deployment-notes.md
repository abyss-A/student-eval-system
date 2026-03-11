# 部署说明

## 1. 本地开发部署
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

## 2. 学校服务器部署建议
### 基本原则
- 不要继续依赖本机绝对路径脚本
- 不要继续使用相对路径 `./uploads` 作为长期部署方案
- 上传目录和数据库必须一起备份

### 推荐目录划分
- 代码目录：例如 `/opt/student-eval/` 或 `D:/student-eval/app/`
- 上传目录：例如 `/data/student-eval/uploads/` 或 `D:/student-eval/data/uploads/`
- 备份目录：例如 `/data/student-eval/backup/`

### 推荐上传目录配置
将后端配置中的：
- `app.upload-dir: ./uploads`
改成服务器固定绝对路径，例如：
- Linux：`/data/student-eval/uploads`
- Windows：`D:/student-eval/data/uploads`

## 3. 数据库与迁移顺序
### 首次部署
1. 创建数据库 `student_eval`
2. 执行 `init.sql`
3. 执行历史迁移脚本（如：`backend/src/main/resources/db/migrations/20260312_scoring_config_subweights.sql`，用于新增评分配置二级占比字段）
4. 再执行 `seed.sql`（如果部署环境需要演示数据）

### 正式环境建议
- 演示环境可以使用种子数据
- 正式环境不要默认跑带演示账号和样本数据的种子脚本，避免污染真实库

## 4. 备份策略
### 必须备份的内容
- MySQL 数据库
- 上传目录 `uploads`
- 报表模板（如果学校后续会自定义）

### 为什么要一起备份
- 数据库只存附件元数据与路径
- 图片本体在磁盘目录中
- 只备份数据库、不备份上传目录，会导致附件记录还在但文件丢失

## 5. 当前脚本的现实限制
### 后端启动脚本
- `backend/scripts/run-backend.ps1` 依赖本机固定 Java 17 路径和 IntelliJ 自带 Maven 路径
- 换机器或上服务器前，必须改掉这些绝对路径

### 数据库初始化脚本
- `backend/scripts/init-db.ps1` 依赖本机固定 MySQL 客户端路径
- 上服务器前，也需要替换成服务器真实路径

## 6. 常见部署风险
- `upload-dir` 用相对路径，导致工作目录一变就找不到附件
- 只迁代码不迁上传目录，导致图片预览失效
- 默认 `mvn` 指到旧 JRE，导致测试或构建失败
- 把演示环境脚本直接用于正式环境，污染正式数据

## 7. 建议的上线前检查清单
- 确认数据库连接信息正确
- 确认上传目录是固定绝对路径且可写
- 确认 Word 模板文件存在于正确位置
- 确认管理员、辅导员、学生三类账号都能登录
- 确认导出、图片预览、审核、再次提交、删除复审链路都正常
- 确认数据库和上传目录备份方案已落地
