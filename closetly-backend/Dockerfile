# Stage 1: Build the application
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Copy only the maven wrapper and pom.xml first to cache dependencies
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

# Copy the source code and build the application
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the compiled jar from the builder stage
COPY --from=builder /app/target/closetly-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# Run using the smaller JRE image
ENTRYPOINT ["java", "-jar", "app.jar"]
