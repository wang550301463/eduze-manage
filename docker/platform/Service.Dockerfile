FROM eclipse-temurin:17-jre-jammy
RUN useradd --system --uid 10001 --create-home eduze && mkdir -p /app /data/media && chown -R eduze:eduze /app /data/media
ARG SERVICE
WORKDIR /app
COPY --chown=eduze:eduze services/${SERVICE}/target/${SERVICE}-1.0.0.jar /app/service.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/service.jar"]
