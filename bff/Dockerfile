FROM eclipse-temurin:21
WORKDIR /app
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "target/bff-0.0.1-SNAPSHOT.jar"]
