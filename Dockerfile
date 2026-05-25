# Paso 1: Usar una imagen ligera de Java 21 
FROM eclipse-temurin:21-jdk-alpine

# Paso 2: Crear un directorio para la aplicación dentro del contenedor
WORKDIR /app

# Paso 3: Copiar el archivo .jar compilado de tu aplicación
COPY target/*.jar app.jar

# Paso 4: Copiar la carpeta de la Wallet desde la raíz del proyecto al contenedor
COPY OracleWallet /app/OracleWallet

# Paso 5: Exponer el puerto 8080 que es donde corre tu Tomcat
EXPOSE 8080

# Paso 6: Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]