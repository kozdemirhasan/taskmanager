# Temel imaj olarak hafif Alpine tabanlı OpenJDK 17 kullan
FROM openjdk:17-jdk-alpine

# Uygulama çalışma dizini
WORKDIR /app

# Host'taki build edilmiş jar'ı konteynıra kopyala
COPY target/task-management-1.0.jar app.jar

# Spring Boot default portunu aç
EXPOSE 8080

# JVM parametreleri için opsiyonel ENV
# ENV JAVA_OPTS=""

# Uygulamayı başlat
ENTRYPOINT ["java","-jar","app.jar"]
