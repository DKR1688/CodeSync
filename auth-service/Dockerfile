FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY src src

RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

CMD ["sh", "-c", "echo Render PORT=$PORT && java -Dserver.port=$PORT -Dserver.address=0.0.0.0 -jar app.jar"]
