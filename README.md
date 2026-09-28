# 今天吃什么

面向校园食堂的口味问卷、菜品筛选与推荐项目。仓库已包含阶段 0 工程基线、阶段 1.2 演示种子数据、阶段 1.3 MyBatis 实体与 Mapper，以及阶段 2 的食堂/档口/菜品/标签基础数据接口。

## 技术栈

- 后端：Java 17、Spring Boot、MyBatis、MySQL
- 前端：Vue 3、TypeScript、Vant、Vue Router、Pinia、Vite
- CI：GitHub Actions

## 本地启动

### 后端

需要 JDK 17+、Maven 3.8+、MySQL 8。先按需执行 `docs/sql/V1.0.0__init.sql`，然后在 `what-to-eat` 目录运行：

```bash
mvn spring-boot:run
```

默认连接 `localhost:3306/what_to_eat`，用户名默认为 `root`。首次建库执行 `docs/sql/V1.0.0__init.sql`，随后执行 `docs/sql/V1.0.1__seed.sql` 导入演示数据。种子数据中的食堂、档口和菜品名称均为虚构示例，接入实际校园前应替换。通过 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`SERVER_PORT` 和 `SPRING_PROFILES_ACTIVE` 环境变量覆盖配置。`GET /api/health` 返回应用健康状态。

### 阶段 2 数据接口

所有接口统一返回 `ApiResponse`。支持食堂、档口、菜品和标签的分页查询及基础增删改查；菜品列表支持按食堂、档口、分类、辣度、餐段、标签和价格范围筛选，`GET/PUT /api/dishes/{id}/tags` 用于读取或替换菜品标签。删除采用逻辑删除。菜品无法在仍有子数据时删除档口，档口无法在仍有菜品时删除食堂。

主要路由：`/api/canteens`、`/api/shops`、`/api/dishes`、`/api/tags`。分页参数为 `page`（默认 1）和 `size`（默认 20，最大 100）。

### 前端

需要 Node.js 20+、npm。进入 `frontend` 目录运行：

```bash
npm ci
npm run dev
```

开发服务器将 `/api` 请求代理到 `http://localhost:8080`。部署时可在 `.env` 中设置 `VITE_API_BASE_URL`。

## 目录约定

```text
src/main/java/com/studyroom/
  ai/          推荐与 Prompt 模块
  common/      统一响应、异常、分页等通用能力
  config/      Spring 配置
  controller/  HTTP 接口入口
  mapper/      MyBatis 数据访问
  model/        持久化模型
  service/      业务逻辑
frontend/src/
  api/ components/ router/ stores/ views/ styles/
docs/sql/      版本化数据库脚本
```

数据库字段与实体按小写下划线和 camelCase 映射；数据关联由应用层维护，不使用物理外键。请将增量变更脚本按版本提交到 `docs/sql`，数据库口令和第三方密钥只通过环境变量提供。
