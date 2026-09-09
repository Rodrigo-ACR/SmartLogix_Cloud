# Etapa 1: Compilar usando la imagen oficial de Maven con Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Imagen final para ejecutar (Ya no necesita instalar Maven)
FROM eclipse-temurin:21
WORKDIR /app
# Copiamos el JAR compilado en la etapa anterior directamente aquí
COPY --from=build /app/target/bff-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]

##original##
#FROM eclipse-temurin:21
#WORKDIR /app
#RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
#COPY pom.xml .
#COPY src ./src
#RUN mvn clean package -DskipTests
#EXPOSE 8085
#ENTRYPOINT ["java", "-jar", "target/bff-0.0.1-SNAPSHOT.jar"]
