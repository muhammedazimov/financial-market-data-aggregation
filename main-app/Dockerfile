FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/main-app-1.0.0.jar app.jar
# Config file should be mounted or copied. 
# We'll copy defaults, but they can be overridden.
COPY src/main/resources/config.properties config.properties
COPY src/main/resources/rate-formulas.json rate-formulas.json
CMD ["java", "-jar", "app.jar"]
