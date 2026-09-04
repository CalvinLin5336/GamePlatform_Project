# 阶段一：用 Maven 編譯打包
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# 阶段二：用輕量級 Java 運行環境執行
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/gameplatform-0.0.1-SNAPSHOT.jar app.jar

# Render 預設會使用 PORT 環境變數
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]