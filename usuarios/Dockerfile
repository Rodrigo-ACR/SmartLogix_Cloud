FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY target/usuarios-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8094
ENTRYPOINT ["java", "-jar", "app.jar"]