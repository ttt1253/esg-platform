# 1. 빌드 단계
FROM gradle:8.5-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon -x test

# 2. 실행 단계 (여기를 바꿨습니다!)
# openjdk 대신 eclipse-temurin (가장 많이 쓰이는 표준 JDK) 사용
FROM eclipse-temurin:17-jdk-jammy
EXPOSE 8080
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]