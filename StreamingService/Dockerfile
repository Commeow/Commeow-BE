FROM eclipse-temurin:17-jdk-jammy

ARG JAR_FILE_PATH=build/libs/*.jar

COPY $JAR_FILE_PATH app.jar

EXPOSE 8080
EXPOSE 1935

ENTRYPOINT ["java", "-jar", "app.jar"]