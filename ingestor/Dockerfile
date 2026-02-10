# --------- СТАДИЯ СБОРКИ ---------
# Объявляем базовый образ и создаем рабочую папку.
FROM maven:3.9.12-eclipse-temurin-21 AS build
WORKDIR /app

# Кешируем зависимости (чтобы ускорить пересборку).
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

# Копируем код сервиса в докер.
COPY src ./src

# Запускаем сборку.
RUN mvn -q -DskipTests package

# --------- СТАДИЯ ЗАПУСКА ---------
# Объявляем базовый образ и создаем рабочую папку.
FROM eclipse-temurin:21-jre
WORKDIR /app

# Переносим собранный jar.
COPY --from=build /app/target/*.jar /app/app.jar

# Объявляем точку входа.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
