FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY src src

RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 10000

CMD ["sh", "-c", "echo PORT_IS_$PORT && java -jar app.jar --server.port=${PORT:-10000} --server.address=0.0.0.0"]
