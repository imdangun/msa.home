FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ARG SERVICE_NAME

# curl 설치 (헬스체크용)
RUN apk add --no-cache curl

# 로컬에서 빌드한 jar 파일 복사
COPY service/${SERVICE_NAME}/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]