# 本地跑通与部署（LOCAL-RUN）

本仓库“今天吃什么”校园食堂推荐系统的本地启动说明。本文记录了**本机环境特有的两个坑**及其修复方式，照做即可一键拉起整套环境（MySQL + 后端 + 前端）。

> 适用环境：Windows + Git Bash，本机已装 Java 18、Node 22、MySQL 5.7（默认 3306，root 有密码），但 **Maven 的 `mvn` 命令损坏**、**Docker 不可用**。

---

## 1. 本机环境的两个坑（必读）

### 坑 A：Maven `mvn` 命令损坏
本机 `mvn` 在 PATH 上指向一个 launcher 损坏的安装（`bin/mvn` 是 Unix 脚本，会把 `/d/...` 路径直接传给 Windows 的 `java.exe`，导致 classworlds 找不到）。

**修复：绕过 `mvn` 脚本，直接调用 Maven 的 classworlds launcher**（用 Windows 路径）：

```bash
MVN_HOME="D:/apache-maven-3.9.14-bin/apache-maven-3.9.14"
PROJ="D:/qq/软件测试/大作业/wte"
java -Dmaven.multiModuleProjectDirectory="$PROJ" \
     -Dmaven.home="$MVN_HOME" \
     -Dclassworlds.conf="$MVN_HOME/bin/m2.conf" \
     -classpath "$MVN_HOME/boot/plexus-classworlds-2.9.0.jar" \
     org.codehaus.plexus.classworlds.launcher.Launcher <maven参数>
```

例如打包：`... launcher.Launcher package -DskipTests`

> 已确认：Maven 本地仓库（`~/.m2/repository`）已被其他同学的“测试”提交填充，依赖无需重新下载；Maven Central 可访问，必要时也能联网拉取。

### 坑 B：MySQL 5.7 默认实例 root 有密码
应用 `application.yml` 默认用 `root` / 空密码连 `localhost:3306/what_to_eat`。本机 3306 上的 MySQL root **有密码**，且里面还有其他库，不应改动。

**修复：起一个隔离的 MySQL 5.7 实例，专供本项目使用**

- 数据目录：`D:/mysql-wte`（已用 `--initialize-insecure` 初始化，root 空密码）
- 端口：**3308**（避开默认 3306）
- 库名：`what_to_eat`

启动隔离实例：

```bash
"D:/MySQL/MySQL Server 5.7/bin/mysqld.exe" --datadir=D:/mysql-wte --port=3308 --console
```

> 注意：必须给 **Windows 路径** `D:/mysql-wte`（不要写 `/d/mysql-wte`，否则 mysqld 会把它当成 `\d\mysql-wte` 而初始化失败）。

建库并灌入种子数据（只需首次）：

```bash
"D:/MySQL/MySQL Server 5.7/bin/mysql.exe" -uroot -P3308 < docs/sql/V1.0.0__init.sql
"D:/MySQL/MySQL Server 5.7/bin/mysql.exe" -uroot -P3308 < docs/sql/V1.0.1__seed.sql
```

---

## 2. 一键启动（推荐）

直接在 Git Bash 里执行本仓库提供的启动脚本：

```bash
bash scripts/run-local.sh
```

该脚本会（如未运行则）依次拉起：

| 服务 | 端口 | 说明 |
|------|------|------|
| MySQL（隔离实例） | 3308 | root 空密码，库 `what_to_eat` |
| 后端 Spring Boot | 8080 | 连 3308，已打好的 fat jar |
| 前端 Vite dev | 5173 | `/api` 代理到 `localhost:8080` |

启动后访问：

- 前端页面：<http://localhost:5173/>
- 后端健康检查：<http://localhost:8080/api/health>
- 经前端代理的 API：<http://localhost:5173/api/health>

---

## 3. 手工分步启动（不依赖脚本）

### 3.1 MySQL（隔离实例，端口 3308）

```bash
"D:/MySQL/MySQL Server 5.7/bin/mysqld.exe" --datadir=D:/mysql-wte --port=3308 --console
```

### 3.2 后端

先打包（如 `target/*.jar` 已存在可跳过）：

```bash
MVN_HOME="D:/apache-maven-3.9.14-bin/apache-maven-3.9.14"
PROJ="D:/qq/软件测试/大作业/wte"
java -Dmaven.multiModuleProjectDirectory="$PROJ" -Dmaven.home="$MVN_HOME" \
     -Dclassworlds.conf="$MVN_HOME/bin/m2.conf" \
     -classpath "$MVN_HOME/boot/plexus-classworlds-2.9.0.jar" \
     org.codehaus.plexus.classworlds.launcher.Launcher package -DskipTests
```

启动（用环境变量覆盖数据库连接，指向隔离实例）：

```bash
cd "D:/qq/软件测试/大作业/wte"
DB_URL="jdbc:mysql://localhost:3308/what_to_eat?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true" \
DB_USERNAME=root DB_PASSWORD= SERVER_PORT=8080 \
java -jar target/what-to-eat-0.1.0-SNAPSHOT.jar
```

验证：`curl http://localhost:8080/api/health` → `{"status":"UP"}`

### 3.3 前端

```bash
cd "D:/qq/软件测试/大作业/wte/frontend"
npm install        # 或 npm ci（需 package-lock 一致）
npm run dev        # Vite，监听 0.0.0.0:5173
```

---

## 4. 冒烟测试（已验证通过）

1. 健康检查：`GET /api/health` → 200 `UP`
2. 代理：`GET http://localhost:5173/api/canteens` → 返回种子食堂（演示一/二/三食堂）
3. 注册：`POST /api/auth/register` → 返回 token
4. 登录：`POST /api/auth/login` → 返回 12h 有效 Bearer Token
5. 鉴权：`GET /api/preferences/me`（带 token）→ 未填问卷时返回 404 “还没有填写口味问卷”（符合预期）

> 说明：Token 存于应用内存，后端重启后全部失效；管理员角色需手动 `UPDATE app_user SET role=2 ...`，不能通过注册指定。

---

## 5. 关闭

- 前端：在 Vite 终端 `Ctrl+C`
- 后端：杀掉 `what-to-eat-0.1.0-SNAPSHOT.jar` 进程（`jps` 查 pid，`taskkill /F /PID <pid>`）
- MySQL：杀掉 `mysqld.exe`（端口 3308 的那个）
