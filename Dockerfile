FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn -q dependency:go-offline -DskipTests

COPY src ./src

RUN mvn clean package -DskipTests && \
    JAR_FILE="$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*.original' | head -n 1)" && \
    cp "$JAR_FILE" app.jar

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
RUN mkdir -p /app/uploads/docs && chown -R spring:spring /app

COPY --from=build /app/app.jar app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]