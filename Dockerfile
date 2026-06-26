# Build stage
FROM maven:3.9.8-eclipse-temurin-22 AS build

WORKDIR /app

# Copy pom.xml first for dependency caching
COPY substacker_java/pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY substacker_java/src ./src

# Build the application
RUN mvn package -DskipTests -B

# Run stage
FROM eclipse-temurin:22-jre

WORKDIR /app

COPY --from=build /app/target/substacker-java-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]