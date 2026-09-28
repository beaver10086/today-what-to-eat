FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
COPY checkstyle.xml .
RUN mvn -B verify

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/what-to-eat-0.1.0-SNAPSHOT.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
