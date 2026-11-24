FROM m.daocloud.io/docker.io/eclipse-temurin:21-jre-jammy

RUN groupadd -r app && useradd -r -g app app

WORKDIR /app

COPY --chown=app:app target/digital-human-conversation-*.jar app.jar

USER app

EXPOSE 8080

# JVM flags live here; K8s must not set command/args that replace this ENTRYPOINT.
# Override at runtime only via env JAVA_OPTS if needed.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
