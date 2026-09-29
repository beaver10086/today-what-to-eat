#!/usr/bin/env bash
# run-local.sh — 一键拉起“今天吃什么”本地环境（MySQL 隔离实例 + 后端 + 前端）
# 用法： bash scripts/run-local.sh
# 适配本机：Maven 的 mvn 命令损坏（改用 classworlds launcher）；MySQL 5.7 默认实例有密码（改用 3308 隔离实例）

set -u

# ---------- 路径配置 ----------
PROJ="D:/qq/软件测试/大作业/wte"
MVN_HOME="D:/apache-maven-3.9.14-bin/apache-maven-3.9.14"
MYSQL_BIN="D:/MySQL/MySQL Server 5.7/bin"
MYSQL_DATADIR="D:/mysql-wte"
MYSQL_PORT=3308
BACKEND_PORT=8080
FRONTEND_PORT=5173

JAR=$(ls "$PROJ"/target/what-to-eat-*.jar 2>/dev/null | head -1)

# ---------- 工具函数 ----------
# 用 HTTP 状态码判断服务是否就绪（忽略 curl 自身非零退出，如本机 curl error 23）
http_ok() {
  local code
  code=$(curl -s -o /dev/null -w "%{http_code}" "$1" 2>/dev/null)
  [ "$code" = "200" ]
}

wait_for() {
  # $1 = url, $2 = 名称, $3 = 最大秒数
  local url="$1" name="$2" max="$3" i=1
  while [ "$i" -le "$max" ]; do
    if http_ok "$url"; then return 0; fi
    i=$((i + 1))
    sleep 1
  done
  echo "      [$name] 等待超时（$max s）"
  return 1
}

echo "==> 项目目录: $PROJ"

# ---------- 1. MySQL（隔离实例 3308） ----------
if "$MYSQL_BIN/mysql.exe" -uroot -P$MYSQL_PORT -e "SELECT 1;" >/dev/null 2>&1; then
  echo "[MySQL] 3308 已在运行，跳过"
else
  echo "[MySQL] 启动隔离实例 (datadir=$MYSQL_DATADIR, port=$MYSQL_PORT) ..."
  if [ ! -d "$MYSQL_DATADIR/mysql" ]; then
    echo "      数据目录未初始化，执行 --initialize-insecure ..."
    "$MYSQL_BIN/mysqld.exe" --initialize-insecure --datadir="$MYSQL_DATADIR" >/dev/null 2>&1
  fi
  "$MYSQL_BIN/mysqld.exe" --datadir="$MYSQL_DATADIR" --port=$MYSQL_PORT --console > "$PROJ/mysql-3308.log" 2>&1 &
  wait_for "http://localhost:$MYSQL_PORT/" "MySQL" 30 || true
  if ! "$MYSQL_BIN/mysql.exe" -uroot -P$MYSQL_PORT -e "USE what_to_eat;" >/dev/null 2>&1; then
    echo "      初始化库与种子数据 ..."
    "$MYSQL_BIN/mysql.exe" -uroot -P$MYSQL_PORT < "$PROJ/docs/sql/V1.0.0__init.sql"
    "$MYSQL_BIN/mysql.exe" -uroot -P$MYSQL_PORT < "$PROJ/docs/sql/V1.0.1__seed.sql"
  fi
  echo "[MySQL] 就绪"
fi

# ---------- 2. 后端 ----------
if [ -n "${JAR:-}" ] && http_ok "http://localhost:$BACKEND_PORT/api/health"; then
  echo "[Backend] $BACKEND_PORT 已在运行，跳过"
else
  if [ -z "${JAR:-}" ]; then
    echo "[Backend] 未找到 fat jar，开始打包 ..."
    java -Dmaven.multiModuleProjectDirectory="$PROJ" \
         -Dmaven.home="$MVN_HOME" \
         -Dclassworlds.conf="$MVN_HOME/bin/m2.conf" \
         -classpath "$MVN_HOME/boot/plexus-classworlds-2.9.0.jar" \
         org.codehaus.plexus.classworlds.launcher.Launcher package -DskipTests
    JAR=$(ls "$PROJ"/target/what-to-eat-*.jar 2>/dev/null | head -1)
  fi
  echo "[Backend] 启动 $JAR (port=$BACKEND_PORT) ..."
  cd "$PROJ"
  DB_URL="jdbc:mysql://localhost:$MYSQL_PORT/what_to_eat?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true" \
  DB_USERNAME=root DB_PASSWORD= SERVER_PORT=$BACKEND_PORT \
  java -jar "$JAR" > "$PROJ/backend.log" 2>&1 &
  wait_for "http://localhost:$BACKEND_PORT/api/health" "Backend" 40
  echo "[Backend] 就绪: http://localhost:$BACKEND_PORT/api/health"
fi

# ---------- 3. 前端 ----------
if http_ok "http://localhost:$FRONTEND_PORT/"; then
  echo "[Frontend] $FRONTEND_PORT 已在运行，跳过"
else
  echo "[Frontend] 启动 Vite dev (port=$FRONTEND_PORT) ..."
  cd "$PROJ/frontend"
  if [ ! -d node_modules ]; then
    echo "      安装依赖 npm install ..."
    npm install
  fi
  npm run dev > "$PROJ/frontend-dev.log" 2>&1 &
  wait_for "http://localhost:$FRONTEND_PORT/" "Frontend" 40
  echo "[Frontend] 就绪: http://localhost:$FRONTEND_PORT/"
fi

echo ""
echo "============================================="
echo " 全部就绪！"
echo " 前端页面 : http://localhost:$FRONTEND_PORT/"
echo " 后端健康 : http://localhost:$BACKEND_PORT/api/health"
echo " API 代理 : http://localhost:$FRONTEND_PORT/api/...  ->  :$BACKEND_PORT"
echo "============================================="
