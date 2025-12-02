# 1. 빌드 단계: Gradle을 이용해서 소스코드를 빌드합니다.
FROM gradle:8.5-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
# 테스트는 건너뛰고 빌드만 빠르게 수행
RUN gradle build --no-daemon -x test

# 2. 실행 단계: 빌드된 결과물(JAR)만 가져와서 가볍게 실행합니다.
FROM openjdk:17-jdk-slim
EXPOSE 8080
# 빌드 단계에서 만들어진 jar 파일을 app.jar라는 이름으로 가져옴
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar
# 실행 명령어
ENTRYPOINT ["java", "-jar", "/app.jar"]