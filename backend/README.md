# 后端专项说明

## 1. 后端职责
后端负责：
- 用户认证与鉴权
- 测评单创建、保存、提交、再次提交
- 辅导员审核与提交管理员
- 管理员查看终审数据
- 公告与反馈接口
- 排名与账号中心相关接口
- 附件上传 / 预览
- Word 报表导出

## 2. 核心目录
- `src/main/java/com/cqnu/eval/controller/`：接口入口
- `src/main/java/com/cqnu/eval/service/`：业务逻辑
- `src/main/java/com/cqnu/eval/mapper/`：数据库访问
- `src/main/java/com/cqnu/eval/model/`：DTO 与实体
- `src/main/java/com/cqnu/eval/security/`：JWT、当前用户上下文、鉴权过滤器
- `src/main/resources/db/`：初始化 SQL、迁移脚本、种子数据
- `src/main/resources/report-templates/`：导出模板
- `scripts/`：数据库初始化、后端启动脚本

## 3. 启动要求
### Java
- 使用 Java 17
- 当前项目脚本默认依赖本机固定 Java 17 路径，换机器时要改脚本中的绝对路径

### Maven
- 项目脚本默认调用本机 IntelliJ 自带 Maven 路径
- 如果系统环境变量中没有 `mvn`，优先使用脚本，不建议手动猜路径

### 数据库
- 默认使用本地 MySQL
- 默认库名：`student_eval`
- 默认账号：`root`
- 默认密码：`123456`

## 4. 常用命令
### 初始化数据库
```powershell
cd backend
powershell -ExecutionPolicy Bypass -File .\scripts\init-db.ps1
```

### 启动后端
```powershell
cd backend
powershell -ExecutionPolicy Bypass -File .\scripts\run-backend.ps1
```

### 运行测试
优先保证 Java 17 生效后再执行：
```powershell
cd backend
mvn -q test
```

## 5. 关键配置说明
配置文件：`src/main/resources/application.yml`

### 数据库
- `spring.datasource.*` 控制 MySQL 连接

### 报表模板
- `report.template.name` 控制当前使用的 Word 模板文件
- 当前正式模板：`scholarship_form_v1.docx`
- 不要把调试模板散落到 `backend/` 根目录

### 上传目录
- `app.upload-dir` 当前默认是 `./uploads`
- 该路径是相对路径，依赖后端启动工作目录
- 本地开发可用，但部署到学校服务器时建议改成固定绝对路径

## 6. 运行数据与源码的边界
### 属于源码或长期配置的内容
- `src/`
- `pom.xml`
- `scripts/`
- `README.md`

### 属于运行期或构建期产物的内容
- `target/`
- `uploads/`
- `*.log`

这些目录和文件不应该作为业务源码理解，也不应该作为功能实现依据。

## 7. 当前重要业务口径
- 学生正式导出 Word：仅在 `COUNSELOR_REVIEWED / FINALIZED / PUBLISHED`
- 管理员主表总分：使用评审生效总分
- 删除流程：先 `DELETE_PENDING_SUBMIT`，再次提交时再进入 `DELETE_REQUESTED`
- 导出模板基础信息区：需要容纳真实班级名，不能被截断

## 8. 后续开发时要注意
- 修改分数显示前先确认是“原始口径”还是“审核生效口径”
- 修改导出逻辑前先确认模板文件和服务口径是否一致
- 修改上传逻辑时要同时考虑数据库记录与磁盘文件路径
- 如果换机器开发，优先先修脚本中的绝对路径依赖