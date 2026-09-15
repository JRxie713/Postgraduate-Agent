FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 复制构建好的 jar
COPY target/scaffold-backend-*.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动
ENTRYPOINT ["java", "-jar", "app.jar"]
