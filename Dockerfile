# ---------- Etapa 1: compilar ----------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY . .

# gradlew puede venir con saltos de linea de Windows; se corrigen para que corra en Linux
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Se omiten los tests para acelerar el build en el servidor
RUN ./gradlew bootJar -x test --no-daemon \
    && cp "$(ls build/libs/*.jar | grep -v plain | head -n 1)" /app/app.jar

# ---------- Etapa 2: ejecutar ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/app.jar app.jar

# Se limita la memoria del JVM para gastar menos credito en el hosting
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-XX:+UseSerialGC", "-jar", "app.jar"]