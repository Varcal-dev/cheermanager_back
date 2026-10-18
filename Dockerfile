# ---- Etapa 1: compilar ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B -DskipTests package && cp target/*.jar app.jar

# ---- Etapa 2: ejecutar ----
FROM eclipse-temurin:21-jre
RUN useradd --system --create-home --uid 10001 cheer
WORKDIR /app
COPY --from=build /app/app.jar app.jar
USER cheer
EXPOSE 8081
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar app.jar"]
