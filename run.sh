#!/usr/bin/env bash
set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

APP_NAME="${APP_NAME:-$(basename "$PROJECT_DIR")}"
LOG_DIR="$PROJECT_DIR/logs"
PID_FILE="$LOG_DIR/${APP_NAME}.pid"
LOG_FILE="$LOG_DIR/${APP_NAME}.out"
STOP_TIMEOUT="${STOP_TIMEOUT:-30}"
JAR_PATH="${JAR_PATH:-}"
JAVA_OPTS="${JAVA_OPTS:--Xms512m -Xmx1024m -Duser.timezone=Asia/Shanghai -Dfile.encoding=UTF-8}"

usage() {
  cat <<EOF
用法:
  ./run.sh build [Maven 参数...]                  编译打包
  ./run.sh run   [选项] [Spring Boot 参数...]     前台运行（默认先编译）
  ./run.sh start [选项] [Spring Boot 参数...]     后台运行（默认先编译）
  ./run.sh stop  [--timeout 秒]                   优雅停止后台进程
  ./run.sh status                                 查看后台进程状态

说明:
  run 是前台进程，Ctrl+C 停止；stop/status 只管理 start 启动的后台进程。
  stop 先发送 SIGTERM，等待 STOP_TIMEOUT 秒后仍未退出才发送 SIGKILL。

选项:
  -h, --help        显示帮助
  -s, --skip-build  run/start 时跳过编译，直接运行已有 jar
  --jar PATH        指定要运行的 jar
  --                其后的参数原样传给 Spring Boot

环境变量:
  JAVA8_HOME        指定 JDK 8 路径
  JAVA_OPTS         JVM 参数，默认 "$JAVA_OPTS"
  STOP_TIMEOUT      优雅停机等待秒数，默认 30
  APP_NAME          应用名，决定 PID 和日志文件名

示例:
  ./run.sh build
  ./run.sh run
  ./run.sh run --skip-build --spring.profiles.active=dev
  ./run.sh start
  ./run.sh status
  ./run.sh stop
  STOP_TIMEOUT=60 ./run.sh stop
  ./run.sh start --skip-build --spring.datasource.initialization-mode=never
EOF
}

java_major() {
  "$1/bin/java" -version 2>&1 | awk -F'"' '/version/ {print $2}' | awk -F. '{if ($1 == "1") print $2; else print $1}'
}

find_jdk8() {
  if [ -n "${JAVA8_HOME:-}" ] && [ -x "$JAVA8_HOME/bin/java" ] && [ "$(java_major "$JAVA8_HOME")" = "8" ]; then
    echo "$JAVA8_HOME"
    return 0
  fi
  local d
  for d in "$HOME"/.sdkman/candidates/java/8* /Library/Java/JavaVirtualMachines/*/Contents/Home; do
    if [ -x "$d/bin/java" ] && [ "$(java_major "$d")" = "8" ]; then
      echo "$d"
      return 0
    fi
  done
  local home
  home="$(/usr/libexec/java_home -v 1.8 2>/dev/null || true)"
  if [ -n "$home" ] && [ -x "$home/bin/java" ]; then
    echo "$home"
    return 0
  fi
  if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ] && [ "$(java_major "$JAVA_HOME")" = "8" ]; then
    echo "$JAVA_HOME"
    return 0
  fi
  if command -v java >/dev/null 2>&1; then
    home="$(java -XshowSettings:properties -version 2>&1 | awk -F'= ' '/java.home =/ {print $2; exit}')"
    if [ -n "$home" ] && [ -x "$home/bin/java" ] && [ "$(java_major "$home")" = "8" ]; then
      echo "$home"
      return 0
    fi
  fi
  return 1
}

setup_java() {
  if ! JDK8_HOME="$(find_jdk8)"; then
    echo "错误: 未找到 JDK 8。本项目使用 Lombok 1.16.20，无法用 JDK 17 编译。" >&2
    echo "请安装 JDK 8，或通过 JAVA8_HOME=/path/to/jdk8 指定。" >&2
    exit 1
  fi
  export JAVA_HOME="$JDK8_HOME"
  export PATH="$JAVA_HOME/bin:$PATH"
}

setup_maven() {
  if [ -x "$PROJECT_DIR/mvnw" ] && [ -f "$PROJECT_DIR/.mvn/wrapper/maven-wrapper.properties" ]; then
    MVN="$PROJECT_DIR/mvnw"
  elif command -v mvn >/dev/null 2>&1; then
    MVN="mvn"
  else
    echo "错误: 未找到 Maven，请先安装 Maven 或补全 mvnw wrapper。" >&2
    exit 1
  fi
}

find_jar() {
  if [ -n "$JAR_PATH" ]; then
    if [ ! -f "$JAR_PATH" ]; then
      echo "错误: 指定的 jar 不存在: $JAR_PATH" >&2
      return 1
    fi
    echo "$JAR_PATH"
    return 0
  fi
  local jars count
  jars="$(find "$PROJECT_DIR/target" -maxdepth 1 -type f -name '*.jar' ! -name '*.original' 2>/dev/null || true)"
  if [ -z "$jars" ]; then
    echo "错误: 未找到可运行的 jar，请先执行 ./run.sh build 或去掉 --skip-build。" >&2
    return 1
  fi
  count="$(printf '%s\n' "$jars" | wc -l | tr -d ' ')"
  if [ "$count" -gt 1 ]; then
    echo "错误: target 下存在多个 jar，请用 --jar 指定：" >&2
    printf '%s\n' "$jars" >&2
    return 1
  fi
  echo "$jars"
}

build_app() {
  echo "==> 使用 JDK 8 ($JAVA_HOME) 编译项目"
  "$MVN" -B -DskipTests clean package "$@"
}

run_app() {
  local jar="$1"
  shift
  echo "==> 前台启动 ${jar##*/}（Ctrl+C 停止）"
  exec "$JAVA_HOME/bin/java" $JAVA_OPTS -jar "$jar" "$@"
}

start_app() {
  local jar="$1"
  shift
  mkdir -p "$LOG_DIR"
  echo "==> 后台启动 ${jar##*/}"
  nohup "$JAVA_HOME/bin/java" $JAVA_OPTS -Dapp.pid.file="$PID_FILE" -jar "$jar" "$@" > "$LOG_FILE" 2>&1 &
  local pid=$!
  echo "$pid" > "$PID_FILE"
  sleep 2
  if ! kill -0 "$pid" 2>/dev/null; then
    echo "错误: 启动失败，日志最后 50 行：" >&2
    tail -n 50 "$LOG_FILE" >&2 || true
    rm -f "$PID_FILE"
    return 1
  fi
  echo "==> 已启动，PID: $pid"
  echo "==> 日志: $LOG_FILE"
}

running_pid() {
  [ -f "$PID_FILE" ] || return 1
  local pid cmd
  pid="$(cat "$PID_FILE" 2>/dev/null || true)"
  case "$pid" in
    ''|0|*[!0-9]*) return 1 ;;
  esac
  if ! kill -0 "$pid" 2>/dev/null; then
    return 1
  fi
  cmd="$(ps -p "$pid" -o command= 2>/dev/null || true)"
  case "$cmd" in
    *"-Dapp.pid.file=$PID_FILE"*) echo "$pid"; return 0 ;;
  esac
  return 1
}

stop_app() {
  local pid
  if ! pid="$(running_pid)"; then
    if [ -f "$PID_FILE" ]; then
      echo "应用未运行，清理失效 PID 文件: $PID_FILE"
      rm -f "$PID_FILE"
    else
      echo "应用未运行"
    fi
    return 0
  fi
  echo "==> 优雅停止，PID: $pid"
  kill -TERM "$pid" 2>/dev/null || true
  local waited=0
  while kill -0 "$pid" 2>/dev/null; do
    if [ "$waited" -ge "$STOP_TIMEOUT" ]; then
      echo "==> ${STOP_TIMEOUT}s 内未退出，执行强制停止"
      kill -KILL "$pid" 2>/dev/null || true
      sleep 1
      break
    fi
    sleep 1
    waited=$((waited + 1))
  done
  if kill -0 "$pid" 2>/dev/null; then
    echo "错误: 进程 $pid 仍未退出" >&2
    return 1
  fi
  rm -f "$PID_FILE"
  echo "==> 已停止"
}

status_app() {
  local pid cmd
  if pid="$(running_pid)"; then
    cmd="$(ps -p "$pid" -o command= 2>/dev/null || true)"
    echo "状态: 运行中"
    echo "PID: $pid"
    if [ -n "$cmd" ]; then
      echo "命令: $cmd"
    fi
    echo "PID 文件: $PID_FILE"
    echo "日志文件: $LOG_FILE"
    return 0
  fi
  echo "状态: 未运行"
  if [ -f "$PID_FILE" ]; then
    echo "PID 文件已失效: $PID_FILE"
  fi
  return 1
}

COMMAND="${1:-}"
if [ $# -gt 0 ]; then
  shift
fi

case "$COMMAND" in
  build)
    setup_java
    setup_maven
    build_app "$@"
    ;;
  run|start)
    SKIP_BUILD=0
    APP_ARGS=()
    while [ $# -gt 0 ]; do
      case "$1" in
        -h|--help)
          usage
          exit 0
          ;;
        -s|--skip-build)
          SKIP_BUILD=1
          shift
          ;;
        --jar)
          shift
          if [ $# -eq 0 ]; then
            echo "错误: --jar 需要指定路径" >&2
            exit 1
          fi
          JAR_PATH="$1"
          shift
          ;;
        --jar=*)
          JAR_PATH="${1#*=}"
          shift
          ;;
        --)
          shift
          APP_ARGS+=("$@")
          break
          ;;
        *)
          APP_ARGS+=("$1")
          shift
          ;;
      esac
    done
    if [ "$COMMAND" = "start" ]; then
      if PID="$(running_pid)"; then
        echo "应用已在运行，PID: $PID"
        exit 0
      fi
    fi
    setup_java
    if [ "$SKIP_BUILD" -eq 0 ]; then
      setup_maven
      build_app
    fi
    if ! JAR="$(find_jar)"; then
      exit 1
    fi
    if [ "$COMMAND" = "run" ]; then
      run_app "$JAR" "${APP_ARGS[@]}"
    else
      start_app "$JAR" "${APP_ARGS[@]}"
    fi
    ;;
  stop)
    while [ $# -gt 0 ]; do
      case "$1" in
        -h|--help)
          usage
          exit 0
          ;;
        --timeout)
          shift
          if [ $# -eq 0 ]; then
            echo "错误: --timeout 需要指定秒数" >&2
            exit 1
          fi
          STOP_TIMEOUT="$1"
          shift
          ;;
        --timeout=*)
          STOP_TIMEOUT="${1#*=}"
          shift
          ;;
        *)
          echo "错误: 未知参数 $1" >&2
          usage
          exit 1
          ;;
      esac
    done
    case "$STOP_TIMEOUT" in
      ''|*[!0-9]*)
        echo "错误: 停止超时时间必须是正整数" >&2
        exit 1
        ;;
    esac
    if [ "$STOP_TIMEOUT" -lt 1 ]; then
      echo "错误: 停止超时时间必须大于 0" >&2
      exit 1
    fi
    stop_app
    ;;
  status)
    while [ $# -gt 0 ]; do
      case "$1" in
        -h|--help)
          usage
          exit 0
          ;;
        *)
          echo "错误: 未知参数 $1" >&2
          usage
          exit 1
          ;;
      esac
    done
    status_app
    ;;
  help|-h|--help)
    usage
    exit 0
    ;;
  "")
    usage
    exit 1
    ;;
  *)
    echo "错误: 未知命令 $COMMAND" >&2
    usage
    exit 1
    ;;
esac
