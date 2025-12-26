# 设置JDK
FROM openjdk:8-jre-alpine

# 工作目录
WORKDIR /app

# 复制应用 JAR 文件
COPY target/*.jar app.jar

# 暴露端口
EXPOSE 8081

# 启动应用
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-jar", "app.jar"]

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1