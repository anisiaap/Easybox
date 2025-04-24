# Use a base image with Java 17+ and Maven
FROM eclipse-temurin:17-jdk

# Set working directory
WORKDIR /app

# Copy the pom.xml and download dependencies first (for cache efficiency)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the entire project and build it
COPY . .
RUN mvn clean install -DskipTests

# Run the Spring Boot app (adjust .jar name if different)
CMD ["java", "-jar", "target/network-0.0.1-SNAPSHOT.jar"]
