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
# Fotos y documentos: carpeta propia de "cheer" y fuera de /app (que es de root). El compose monta aqui el volumen
# `uploads`; un volumen nuevo hereda este dueno, asi que la API puede escribir y los archivos sobreviven a recrear el contenedor.
RUN mkdir -p /data/uploads && chown cheer:cheer /data/uploads
ENV UPLOAD_DIR=/data/uploads
WORKDIR /app
COPY --from=build /app/app.jar app.jar
USER cheer
EXPOSE 8081
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar app.jar"]
