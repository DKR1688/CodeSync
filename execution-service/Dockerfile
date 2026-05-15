FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jdk
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    cargo \
    golang-go \
    nodejs \
    npm \
    php-cli \
    python3 \
    ruby-full \
    rustc \
    && npm install -g typescript \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/target/execution-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
