# 测试环境部署

本地安装 Docker Compose 后，在仓库根目录启动测试环境：

```bash
docker compose -f docker-compose.test.yml up --build -d
```

应用健康检查地址为 `http://localhost:8080/api/health`（期望 HTTP 200 且 `data.status` 为 `UP`），前端地址为 `http://localhost:8081`。测试库首次启动时会自动执行 `docs/sql/V1.0.0__init.sql` 和 `docs/sql/V1.0.1__seed.sql`。要重建测试库，可先停止服务并删除 `mysql-test-data` 卷；这会清除该测试环境数据。

`MYSQL_ROOT_PASSWORD` 可通过环境变量覆盖。默认口令只用于隔离的本机测试，请勿用于共享环境。CI 会独立创建短生命周期数据库服务，并执行同一组 schema/seed 脚本。
