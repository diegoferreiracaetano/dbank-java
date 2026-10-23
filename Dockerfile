FROM eclipse-temurin:21-jdk AS build
ARG MODULE
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.gradle,sharing=locked ./gradlew ":${MODULE}:bootJar" --no-daemon
RUN cp "$(find "${MODULE}/build/libs" -name '*.jar' ! -name '*-plain.jar')" /tmp/application.jar \
    && java -Djarmode=tools -jar /tmp/application.jar extract --layers --destination /tmp/extracted

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /application
COPY --from=build --chown=app:app /tmp/extracted/dependencies/ ./
COPY --from=build --chown=app:app /tmp/extracted/spring-boot-loader/ ./
COPY --from=build --chown=app:app /tmp/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /tmp/extracted/application/ ./
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["java", "-jar", "application.jar"]
