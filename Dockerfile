# Build stage
FROM maven:3.9.8-eclipse-temurin-22 AS build
WORKDIR /app
COPY substacker_java/pom.xml substacker_java/pom.xml
# Download dependencies first (cached layer)
RUN mvn dependency:go-offline -B
COPY substacker_java/src/main/java ./substaker_java/src/main/java
RUN mvn package -DskipTests -B

# Run stage
FROM eclipse-temurin:22-jre
WORKDIR /app
COPY --from=build /app/target/substacker-java-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
