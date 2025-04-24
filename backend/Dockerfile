# Use an official Maven image with JDK 17 (you can also use 21 if needed)
FROM maven:3.9.5-eclipse-temurin-17 AS build

# Set workdir
WORKDIR /app

# Copy pom and download dependencies first (for cache efficiency)
COPY backend/pom.xml .

# Download dependencies (won’t copy source yet)
RUN mvn dependency:go-offline -B

# Now copy the source code
COPY backend .

# Package the application (skip tests for speed)
RUN mvn package -DskipTests

# Use JDK runtime to run the app
FROM eclipse-temurin:17-jre

# Copy only the final jar from the builder stage
COPY --from=build /app/target/*.jar app.jar

# Run the jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
