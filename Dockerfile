# Spring Boot + Vaadin (Gradle) runtime image
FROM eclipse-temurin:21-jdk-alpine

# Copy Gradle-built jar (exclude -plain.jar)
COPY build/libs/TestMyLanguage-*-SNAPSHOT.jar /app.jar

# Default Spring Boot port (can be overridden)
EXPOSE 8080

# Run with production profile to enable Vaadin production mode
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "/app.jar"]
