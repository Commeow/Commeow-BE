FROM eclipse-temurin:17-jdk-jammy

RUN apt-get update && apt-get install -y ffmpeg

ARG JAR_FILE_PATH=build/libs/*.jar

COPY $JAR_FILE_PATH app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]