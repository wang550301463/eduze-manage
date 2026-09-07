# 生产镜像：先本地 mvn package 生成 jar，再构建镜像
#   ./mvnw -DskipTests package
#   docker build -f docker/Dockerfile -t eduze-manage:latest .

FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd -r eduze && useradd -r -g eduze eduze \
    && mkdir -p /app /data/storage /app/logs \
    && chown -R eduze:eduze /app /data/storage /app/logs

WORKDIR /app

COPY target/eduze-manage.jar /app/app.jar

USER eduze

ENV SPRING_PROFILES_ACTIVE=prod \
    EDUZE_STORAGE_LOCAL_ROOT=/data/storage

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
