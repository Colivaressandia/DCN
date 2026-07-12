# Usa la imagen base
FROM eclipse-temurin:21-jre-jammy

# Crea el directorio de trabajo
WORKDIR /app

# Copia el JAR generado por Maven
COPY target/inscripciones-0.0.1-SNAPSHOT.jar app.jar

# COPIA EL WALLET 
COPY OracleWallet /app/OracleWallet

# Punto de entrada
ENTRYPOINT ["java", "-jar", "app.jar"]