# Use a lightweight OpenJDK image
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy the built JAR into the image
COPY graphql-app/target/graphql-app-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# Start the app with the postgres profile
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=postgres"] 