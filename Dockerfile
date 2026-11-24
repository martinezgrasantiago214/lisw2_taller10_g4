# syntax=docker/dockerfile:1
#
# Imagen generica para cualquiera de los microservicios (multi-etapa).
# No hace falta tener Maven ni el JDK instalados: la compilacion ocurre dentro
# de Docker. docker-compose.yml construye esta imagen una vez por servicio con
# el argumento SERVICE; la etapa "build" es identica para todos, asi que
# Docker la ejecuta una sola vez y la reutiliza (cache).
#
#   docker build --build-arg SERVICE=review-service -t saberpro/review-service .

# ---- Etapa 1: compilar todo el backend con Maven ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
# La cache de ~/.m2 sobrevive entre construcciones (BuildKit): las dependencias
# se descargan una sola vez. El frontend Swing no se empaqueta en contenedores.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -DskipTests -pl '!frontend' -am package

# ---- Etapa 2: imagen de ejecucion liviana (solo JRE) ----
FROM eclipse-temurin:17-jre AS runtime
ARG SERVICE
ARG VERSION=3.0.0
LABEL org.opencontainers.image.title="saberpro-${SERVICE}" \
      org.opencontainers.image.description="Banco de Preguntas Saber PRO - ${SERVICE}" \
      org.opencontainers.image.version="${VERSION}"
# Usuario sin privilegios; /data y /outbox le pertenecen para que los
# volumenes de las bases de datos SQLite y de la bandeja de correo sean escribibles.
RUN groupadd --system saberpro && useradd --system --gid saberpro --home /app saberpro \
    && mkdir -p /app /data /outbox && chown -R saberpro:saberpro /app /data /outbox
WORKDIR /app
COPY --from=build --chown=saberpro:saberpro /workspace/${SERVICE}/target/${SERVICE}-${VERSION}.jar app.jar
USER saberpro
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
