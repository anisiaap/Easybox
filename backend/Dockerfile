# Use Maven with JDK 17 for building
FROM maven:3.9.5-eclipse-temurin-17 AS build

WORKDIR /app

# Copy *only* current folder contents (because you're already inside `backend/`)
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY . .
RUN mvn package -DskipTests

# Use lightweight JRE image
FROM eclipse-temurin:17-jre

COPY --from=build /app/target/network-0.0.1-SNAPSHOT.jar app.jar

CMD ["java", "-jar", "/app.jar"]

