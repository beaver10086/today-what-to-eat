# 今天吃什么

面向校园食堂的口味问卷、菜品筛选与推荐项目。仓库包含阶段 0 工程基线、阶段 1 数据层、阶段 2 数据接口与管理页面、阶段 3 注册登录与口味画像、阶段 4 个性化推荐、阶段 5 收藏与前端整合，以及阶段 6 的质量检查与测试部署配置。

## 技术栈

- 后端：Java 17、Spring Boot、MyBatis、MySQL
- 前端：Vue 3、TypeScript、Vant、Vue Router、Pinia、Vite
- CI：GitHub Actions

## v1.1 菜单数据维护

- 档口可维护排队热度（无需排队、较短、较长、很长），菜品可维护打包适配度（不适合、一般、适合）和信息来源；未核实的值可留空。档口与菜品详情页会展示已填写的信息。
- 管理员后台的菜品列表包含已下架菜品，可继续编辑或上架。新增“下载 CSV 模板”和“导入菜品 CSV”；导入仅新增菜品，不修改或删除现有记录，并记录导入操作。
- 导入前校验整份 UTF-8 CSV：检查档口、价格、字段范围及重复菜品。任一行有误则整批不写入，并返回行号；单次最多 500 道菜、512 KB，支持带引号的逗号和换行。先在后台创建档口，再按以下表头填写。必填列为 `shopId`、`dishName`、`price`、`category` 和 `dataSource`；`category` 为 1–7，`mealType` 为 1–15 的餐段位掩码，`spiceLevel` 为 0–3，`takeoutSuitability` 为 0–2，`isAvailable` 为 0 下架或 1 上架。
- 再次保存口味问卷时，保留此前由反馈形成的口味权重调整。

```csv
shopId,dishName,price,category,mealType,spiceLevel,takeoutSuitability,dataSource,calorie,description,imageUrl,isAvailable
910001,番茄炒蛋盖饭,12.50,1,2,0,2,实地核对,450,午餐示例,,1
```

升级已有数据库需执行一次 `docs/sql/V1.0.3__menu_metadata.sql`。v1.1 提供真实菜单的维护与导入入口，仓库中的演示菜单仍是虚构数据，真实菜单需要核对后自行导入。

## 本地启动

### 后端

需要 JDK 17+、Maven 3.8+、MySQL 8。首次建库后执行 `docs/sql/V1.0.0__init.sql`；需要演示数据时再执行 `docs/sql/V1.0.1__seed.sql` 和 `docs/sql/V1.0.2__accounts.sql`。已有数据库只需执行一次增量脚本 `docs/sql/V1.0.3__menu_metadata.sql`；新库在建表后、启动应用前也需执行它。然后在 `what-to-eat` 目录运行：

```bash
mvn spring-boot:run
```

默认连接 `localhost:3306/what_to_eat`，用户名默认为 `root`。种子数据中的食堂、档口和菜品名称均为虚构示例，接入实际校园前应替换。通过 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`SERVER_PORT` 和 `SPRING_PROFILES_ACTIVE` 环境变量覆盖配置。`GET /api/health` 返回应用健康状态。运行后端单元测试与静态检查：

```bash
mvn verify
```

可选的大模型服务使用根目录环境变量 `AI_BASE_URL`、`AI_API_KEY`、`AI_MODEL` 接入 OpenAI 兼容的 Chat Completions API。未配置时，规则解析和目录检索问答仍可使用。密钥通过本地环境变量或部署密钥提供，不提交到仓库。

### 阶段 2 数据接口

所有接口统一返回 `ApiResponse`。支持食堂、档口、菜品和标签的分页查询及基础增删改查；菜品列表支持按食堂、档口、分类、辣度、餐段、标签和价格范围筛选，`GET/PUT /api/dishes/{id}/tags` 用于读取或替换菜品标签。删除采用逻辑删除。菜品无法在仍有子数据时删除档口，档口无法在仍有菜品时删除食堂。

主要路由：`/api/canteens`、`/api/shops`、`/api/dishes`、`/api/tags`。分页参数为 `page`（默认 1）和 `size`（默认 20，最大 100）。`GET /api/dishes/random` 可使用同一组菜品筛选条件随机抽取一道在售菜品；前端可浏览全部菜品、进入菜品详情并沿食堂/档口层级查看菜单。食堂、档口、菜品及标签的写操作需管理员 Token。

### 阶段 3 用户与口味

`POST /api/auth/register` 和 `POST /api/auth/login` 返回 12 小时有效的 Bearer Token。注册账号默认是普通用户；管理员角色需由受信任的数据库维护流程授予，不能通过注册请求指定。Token 当前存于应用内存，服务重启后全部失效。登录后的用户可通过 `GET/PUT /api/preferences/me` 保存或修改问卷，`GET /api/preferences/me/profile` 获取由标签权重生成的画像。

本地开发需要访问后台时，可先注册并登录，再由维护者在开发数据库中将指定账号升级为管理员：`UPDATE app_user SET role = 2 WHERE username = '替换为已核验的用户名' AND is_deleted = 0;`。不要在生产环境直接复制此操作。

前端页面：`/login` 注册登录、`/survey` 口味问卷、`/profile` 画像、`/admin` 后台维护（仅管理员角色可进入）。

### 菜品目录、自然语言筛选与智能助手

首页显示全部在售菜品并支持分页/筛选；从食堂入口进入门店清单，再进入门店菜单和菜品详情。`POST /api/assistant/search` 将自然语言解析成预算、辣度、餐段、分类、食堂/门店和菜名条件，再用菜品库执行筛选；`POST /api/assistant/ask` 检索当前食堂、门店和菜品资料，返回文字答案与可跳转菜品详情的卡片。

全局悬浮“问助手”入口可在首页和详情页打开。配置模型后，助手会基于检索到的目录资料组织回答并解析筛选条件；模型未配置或暂时不可用时，退回规则解析和目录结果摘要。“附近”会被识别为意图，但当前表结构没有宿舍与食堂坐标，系统会明确提示无法按实际步行距离排序。

### 阶段 4 推荐、收藏与菜品详情

`/recommendations` 按餐段结合口味画像、辣度、预算、忌口标签与菜品评分给出推荐；用户可反馈喜好或换一组，避免再次出现上一组菜品。未登录用户也可在菜品发现页用当前筛选条件进行随机推荐。菜品卡片可打开 `/dishes/:id`，查看菜品描述、价格、热量、餐段、标签和所属食堂/档口；从详情可以继续查看档口菜单。

### 阶段 6 质量检查

`mvn verify` 执行后端单元/Mapper 测试、Checkstyle 和 JaCoCo 覆盖率门禁（服务包 70%，推荐服务 60%）。前端执行 `npm run lint`、`npm run build` 和 `npm run test:e2e`；Playwright 在桌面 Chromium、iPhone WebKit 和 Pixel Chromium 配置下运行响应式冒烟并保存截图。Newman API 回归位于 `tests/newman`，30 并发 JMeter 基准位于 `tests/jmeter`。完整运行方式、性能阈值、SonarQube 设置和测试环境启动步骤见 [阶段 6 质量与部署说明](docs/phase-6-test-plan.md) 与 [测试环境部署](docs/deployment-test.md)。

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
