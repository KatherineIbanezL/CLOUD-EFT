# --- Etapa 1: Compilar con Maven ---
FROM eclipse-temurin:21-jdk AS buildstage

RUN apt-get update && apt-get install -y maven

WORKDIR /app

COPY pom.xml .
COPY src /app/src

RUN mvn clean package -DskipTests

# --- Etapa 2: Imagen optimizada y ligera ---
FROM eclipse-temurin:21-jre 

WORKDIR /app

# Ejecutable desde la etapa de compilación
COPY --from=buildstage /app/target/*.jar /app/app.jar

# Trae la Wallet desde el buildstage para evitar errores de ruta en GitHub Actions
COPY --from=buildstage /app/src/main/resources/Wallet_MBD /app/src/main/resources/Wallet_MBD

# Directorio local para enlazar el montaje de Amazon EFS
RUN mkdir -p /app/efs

EXPOSE 8082

CMD ["java", "-jar", "/app/app.jar"]