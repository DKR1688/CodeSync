FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/target/collab-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8084

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
