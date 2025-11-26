FROM gradle:8.10-jdk21 AS builder
WORKDIR /app

ARG SERVICE_NAME

COPY settings.gradle.kts ./
COPY build.gradle.kts ./
COPY gradle ./gradle
COPY gradlew ./
COPY service/${SERVICE_NAME}/build.gradle.kts ./service/${SERVICE_NAME}/
COPY service/${SERVICE_NAME}/src ./service/${SERVICE_NAME}/src

RUN ./gradlew :${SERVICE_NAME}:bootJar --no-daemon
RUN java -Djarmode=layertools -jar service/${SERVICE_NAME}/build/libs/*.jar extract

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl
COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]