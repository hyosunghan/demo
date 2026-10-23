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
  JAVA_VERSION      强制指定 Java 主版本，如 8、11、17
  JAVA_HOME         优先使用的 JDK 路径（版本需与项目要求一致）
  JAVA_OPTS         JVM 参数，默认 "$JAVA_OPTS"
  STOP_TIMEOUT      优雅停机等待秒数，默认 30
  APP_NAME          应用名，决定 PID 和日志文件名

Java 版本读取顺序:
  JAVA_VERSION > .java-version > .sdkmanrc > .tool-versions > pom.xml
  未读取到版本时，使用 JAVA_HOME 或 PATH 中的 java。

示例:
  ./run.sh build
  JAVA_VERSION=8 ./run.sh build
  ./run.sh run
  ./run.sh run --skip-build --spring.profiles.active=dev
  ./run.sh start
  ./run.sh status
  ./run.sh stop
  STOP_TIMEOUT=60 ./run.sh stop
  ./run.sh start --skip-build --spring.datasource.initialization-mode=never
EOF
}

normalize_major() {
  local v
  v="$(printf '%s\n' "$1" | grep -Eo '(^|[-_])[0-9]+(\.[0-9]+)*' | head -n 1 | sed 's/^[-_]//')"
  if [ -z "$v" ]; then
    return 1
  fi
  case "$v" in
    1.*)
      v="${v#1.}"
      v="${v%%.*}"
      ;;
    *)
      v="${v%%.*}"
      ;;
  esac
  case "$v" in
    ''|*[!0-9]*) return 1 ;;
  esac
  echo "$v"
}

java_major() {
  local v
  v="$("$1/bin/java" -version 2>&1 | awk -F'"' '/version/ {print $2; exit}')"
  normalize_major "$v"
}

jdk_major_is() {
  local home="${1:-}" expected="$2" actual
  [ -n "$home" ] && [ -x "$home/bin/java" ] || return 1
  actual="$(java_major "$home" 2>/dev/null || true)"
  [ "$actual" = "$expected" ]
}

find_jdk_for_version() {
  local required="$1" d home lookup
  if jdk_major_is "${JAVA_HOME:-}" "$required"; then
    echo "$JAVA_HOME"
    return 0
  fi
  for d in \
    "$HOME"/.sdkman/candidates/java/* \
    "$HOME"/.jdks/* \
    "$HOME"/Library/Java/JavaVirtualMachines/*/Contents/Home \
    /Library/Java/JavaVirtualMachines/*/Contents/Home \
    /usr/lib/jvm/* \
    /opt/homebrew/opt/openjdk*/libexec/openjdk.jdk/Contents/Home \
    /usr/local/opt/openjdk*/libexec/openjdk.jdk/Contents/Home; do
    if jdk_major_is "$d" "$required"; then
      echo "$d"
      return 0
    fi
  done
  lookup="$required"
  if [ "$required" = "8" ]; then
    lookup="1.8"
  fi
  if [ -x /usr/libexec/java_home ]; then
    home="$(/usr/libexec/java_home -v "$lookup" 2>/dev/null || true)"
    if jdk_major_is "$home" "$required"; then
      echo "$home"
      return 0
    fi
  fi
  if command -v java >/dev/null 2>&1; then
    home="$(java -XshowSettings:properties -version 2>&1 | awk -F'= ' '/java.home =/ {print $2; exit}')"
    if jdk_major_is "$home" "$required"; then
      echo "$home"
      return 0
    fi
  fi
  return 1
}

find_any_jdk() {
  local home
  if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    echo "$JAVA_HOME"
    return 0
  fi
  if command -v java >/dev/null 2>&1; then
    home="$(java -XshowSettings:properties -version 2>&1 | awk -F'= ' '/java.home =/ {print $2; exit}')"
    if [ -n "$home" ] && [ -x "$home/bin/java" ]; then
      echo "$home"
      return 0
    fi
  fi
  return 1
}

pom_property() {
  local tag="$1"
  [ -f "$PROJECT_DIR/pom.xml" ] || return 1
  awk -v tag="$tag" '
    index($0, "<" tag ">") {
      line = $0
      sub(".*<" tag ">", "", line)
      sub("</" tag ">.*", "", line)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", line)
      print line
      exit
    }
  ' "$PROJECT_DIR/pom.xml"
}

detect_java_version() {
  local v tag
  if [ -f "$PROJECT_DIR/.java-version" ]; then
    v="$(awk 'NF && $1 !~ /^#/ {print $1; exit}' "$PROJECT_DIR/.java-version")"
    if [ -n "$v" ] && v="$(normalize_major "$v")"; then
      echo "$v"
      return 0
    fi
  fi
  if [ -f "$PROJECT_DIR/.sdkmanrc" ]; then
    v="$(awk -F= '/^[[:space:]]*java[[:space:]]*=/{gsub(/[[:space:]]/,"",$2); print $2; exit}' "$PROJECT_DIR/.sdkmanrc")"
    if [ -n "$v" ] && v="$(normalize_major "$v")"; then
      echo "$v"
      return 0
    fi
  fi
  if [ -f "$PROJECT_DIR/.tool-versions" ]; then
    v="$(awk '$1 == "java" {print $2; exit}' "$PROJECT_DIR/.tool-versions")"
    if [ -n "$v" ] && v="$(normalize_major "$v")"; then
      echo "$v"
      return 0
    fi
  fi
  for tag in java.version maven.compiler.release maven.compiler.source maven.compiler.target; do
    v="$(pom_property "$tag" || true)"
    if [ -z "$v" ]; then
      continue
    fi
    case "$v" in
      *'${'*) continue ;;
    esac
    if v="$(normalize_major "$v")"; then
      echo "$v"
      return 0
    fi
  done
  return 1
}

setup_java() {
  local required home
  if [ -n "${JAVA_VERSION:-}" ]; then
    if ! required="$(normalize_major "$JAVA_VERSION")"; then
      echo "错误: JAVA_VERSION 格式无效: $JAVA_VERSION" >&2
      exit 1
    fi
  else
    required="$(detect_java_version || true)"
  fi

  if [ -n "$required" ]; then
    if ! home="$(find_jdk_for_version "$required")"; then
      echo "错误: 项目要求 JDK $required，但本机未找到。" >&2
      echo "请安装 JDK $required，或设置 JAVA_HOME 指向对应版本。" >&2
      exit 1
    fi
  else
    if ! home="$(find_any_jdk)"; then
      echo "错误: 未找到可用的 JDK，请设置 JAVA_HOME。" >&2
      exit 1
    fi
    echo "==> 项目未配置 Java 版本，使用当前 JDK: $home"
  fi

  export JAVA_HOME="$home"
  export PATH="$JAVA_HOME/bin:$PATH"
  echo "==> 使用 JDK $(java_major "$JAVA_HOME"): $JAVA_HOME"
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
  echo "==> 编译项目"
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
