# ---- Build stage --------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Warm the dependency cache first so code changes don't re-download the world.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B package -DskipTests

# ---- Runtime stage -------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system converge && useradd --system --gid converge converge
USER converge

COPY --from=build /app/target/convergeai-backend-*.jar app.jar

EXPOSE 8080
# Container-aware JVM sizing: Render free tier gives 512 MB.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
