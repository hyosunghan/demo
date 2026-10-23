#!/usr/bin/env bash
set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

usage() {
  cat <<EOF
用法: ./run.sh [选项] [Spring Boot 参数...]

选项:
  -h, --help        显示帮助
  -s, --skip-build  跳过 Maven 编译，直接运行已有的 jar
  --                其后的参数原样传给 Spring Boot

环境变量:
  JAVA8_HOME        指定 JDK 8 路径（本项目 Lombok 1.16.20 需要 JDK 8）
  JAVA_OPTS         JVM 参数，默认 "-Xms512m -Xmx1024m -Duser.timezone=Asia/Shanghai -Dfile.encoding=UTF-8"

示例:
  ./run.sh
  ./run.sh --spring.profiles.active=dev
  ./run.sh --skip-build
  ./run.sh --skip-build --spring.datasource.initialization-mode=never   # 数据库不可达时跳过建表
  JAVA_OPTS="-Xmx2g" ./run.sh
EOF
}

java_major() {
  "$1/bin/java" -version 2>&1 | awk -F'"' '/version/ {print $2}' | awk -F. '{if ($1 == "1") print $2; else print $1}'
}

find_jdk8() {
  if [ -n "${JAVA8_HOME:-}" ] && [ -x "$JAVA8_HOME/bin/java" ]; then
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

if ! JDK8_HOME="$(find_jdk8)"; then
  echo "错误: 未找到 JDK 8。本项目使用 Lombok 1.16.20，无法用 JDK 17 编译。" >&2
  echo "请安装 JDK 8，或通过 JAVA8_HOME=/path/to/jdk8 指定。" >&2
  exit 1
fi
export JAVA_HOME="$JDK8_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

if [ -x "$PROJECT_DIR/mvnw" ] && [ -f "$PROJECT_DIR/.mvn/wrapper/maven-wrapper.properties" ]; then
  MVN="$PROJECT_DIR/mvnw"
elif command -v mvn >/dev/null 2>&1; then
  MVN="mvn"
else
  echo "错误: 未找到 Maven，请先安装 Maven 或补全 mvnw wrapper。" >&2
  exit 1
fi

if [ "$SKIP_BUILD" -eq 0 ]; then
  echo "==> 使用 JDK 8 ($JAVA_HOME) 编译项目"
  "$MVN" -B -DskipTests clean package
fi

JAR=""
for f in "$PROJECT_DIR"/target/*.jar; do
  case "$f" in
    *.original) continue ;;
  esac
  if [ -f "$f" ]; then
    JAR="$f"
  fi
done

if [ -z "$JAR" ] || [ ! -f "$JAR" ]; then
  echo "错误: 未找到可运行的 jar，请先执行编译或去掉 --skip-build。" >&2
  exit 1
fi

JAVA_OPTS="${JAVA_OPTS:--Xms512m -Xmx1024m -Duser.timezone=Asia/Shanghai -Dfile.encoding=UTF-8}"

echo "==> 启动 ${JAR##*/}"
echo "==> 应用地址: http://localhost:8081/"
exec "$JAVA_HOME/bin/java" $JAVA_OPTS -jar "$JAR" "${APP_ARGS[@]}"
