# 서버 실행 이미지. Gradle 빌드는 배포하는 PC 에서 끝내고(deploy.sh), 여기서는 실행본만 담는다.
# 1GB 인스턴스에서 Gradle 을 돌리면 메모리가 모자라고, 같은 인스턴스에 Talk 서버도 떠 있어 힙을 작게 잡는다.
FROM eclipse-temurin:17-jre-jammy

RUN useradd --system --uid 1001 ods

COPY --chown=ods server /app

USER ods
ENV PORT=8080 \
    JAVA_OPTS="-Xms64m -Xmx160m -XX:MaxMetaspaceSize=96m -Xss512k -XX:+UseSerialGC -Dfile.encoding=UTF-8"
EXPOSE 8080
ENTRYPOINT ["/app/bin/server"]
