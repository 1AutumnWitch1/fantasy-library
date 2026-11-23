FROM mcr.microsoft.com/openjdk/jdk:21-ubuntu AS build
WORKDIR /build
ADD --checksum=sha256:80ffca22aed9e8b9713a232f3394fd81d7f20322df75efdb2b047dbd3e3a23bb https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.tar.gz /tmp/maven.tar.gz
RUN mkdir /opt/maven \
    && tar -xzf /tmp/maven.tar.gz -C /opt/maven --strip-components=1 \
    && rm /tmp/maven.tar.gz
COPY pom.xml ./
COPY src/ ./src/
RUN --mount=type=cache,target=/root/.m2/repository \
    /opt/maven/bin/mvn --batch-mode --no-transfer-progress package \
    && cp target/fantasy-library-backend-*.jar /build/application.jar

FROM mirror.gcr.io/library/eclipse-temurin:21-jre-noble
WORKDIR /app
RUN groupadd --gid 10001 library \
    && useradd --uid 10001 --gid library --no-create-home --shell /usr/sbin/nologin library \
    && install -d -o library -g library /app/uploads/community /app/uploads/game-downloads
COPY --from=build /build/application.jar /app/application.jar
ENV SERVER_PORT=8080 JAVA_TOOL_OPTIONS="-Djava.awt.headless=true -Dfile.encoding=UTF-8"
USER library:library
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=8 \
    CMD curl --fail --silent --show-error --max-time 4 "http://127.0.0.1:${SERVER_PORT}/api/health" | grep -q '"database":"UP"'
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
