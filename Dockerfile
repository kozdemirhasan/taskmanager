# Build aşaması için JDK 17 kullanan bir base image
FROM eclipse-temurin:17-jdk-focal as builder

# Çalışma dizinini ayarla
WORKDIR /app

# Maven wrapper ve pom.xml dosyasını kopyala
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Maven wrapper'ı çalıştırılabilir yap
RUN chmod +x ./mvnw

# Bağımlılıkları indir
RUN ./mvnw dependency:go-offline

# Kaynak kodları kopyala
COPY src ./src/

# Uygulamayı derle
RUN ./mvnw package -DskipTests

# Çalışma aşaması için JRE 17 kullanan minimal bir image
FROM eclipse-temurin:17-jre-focal

# Çalışma dizinini ayarla
WORKDIR /app

# Builder aşamasından JAR dosyasını kopyala
COPY --from=builder /app/target/*.jar app.jar

# Render'ın kullanacağı port
ENV PORT=8080

# Uygulamayı başlat
CMD ["java", "-jar", "app.jar"]