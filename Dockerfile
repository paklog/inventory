# Use a base image with Java 17
FROM openjdk:17-jdk-slim

# Set the working directory in the container
WORKDIR /app

# Copy the Maven wrapper files to the container
COPY mvnw .
COPY .mvn .mvn

# Copy the pom.xml file
COPY pom.xml .

# Build the application - this will download dependencies and compile code
# Use --batch-mode to prevent interactive prompts
# Use -DskipTests to skip tests during the build (tests will be run in CI/CD)
RUN ./mvnw clean install -DskipTests

# Copy the generated JAR file
# The JAR file will be in target/inventory-0.0.1-SNAPSHOT.jar
COPY target/inventory-0.0.1-SNAPSHOT.jar app.jar

# Expose the port the application runs on
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]