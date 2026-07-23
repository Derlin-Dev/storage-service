# ── Etapa 1: build con Maven ─────────────────────────────
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copiamos primero los archivos de dependencias para aprovechar la cache de Docker:
# si el pom.xml no cambia, esta capa no se vuelve a bajar en cada build.
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ── Etapa 2: imagen final, solo el JRE + el jar ──────────
FROM eclipse-temurin:21-jre
WORKDIR /app

# Usuario no-root por seguridad
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]