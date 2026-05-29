# ==========================================
# Stage 1: Build Stage (Maven)
# ==========================================
FROM maven:3.9.16-eclipse-temurin-21 AS builder

# Set the Working Directory in Container
WORKDIR /app

# Copy pom.xml file in Container 
COPY pom.xml .

# Copy src directory in Container 
COPY src ./src

# Run the maven build command
RUN mvn clean package -DskipTests=true

# ==========================================
# Stage 2: Final Run Stage (JRE Alpine)
# ==========================================
FROM eclipse-temurin:21-jre-alpine

# Set the Working Directory in Container
WORKDIR /app

# Copy the built JAR file from the builder stage
COPY --from=builder /app/target/*.jar /app/target/*.jar

# Expose the container port 8080
EXPOSE 8080

# Run the JAR file
ENTRYPOINT ["java", "-jar", "/app/target/*.jar"]
