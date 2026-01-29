# Multi-stage сборка для уменьшения размера финального образа
# Этап 1: Сборка приложения с Maven
FROM maven:3.9-eclipse-temurin-17 AS builder

# Установка рабочей директории
WORKDIR /app

# Копирование файлов проекта для кэширования зависимостей
COPY pom.xml .
# Скачивание зависимостей (кэшируется, если не меняется pom.xml)
RUN mvn dependency:go-offline -B

# Копирование исходного кода
COPY src ./src

# Сборка приложения (пропускаем тесты для ускорения сборки)
RUN mvn clean package -DskipTests

# Этап 2: Создание финального образа
FROM eclipse-temurin:17-jre-alpine

# Установка рабочей директории
WORKDIR /app

# Создание пользователя для безопасности (не запускать от root)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Копирование собранного JAR из этапа сборки
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

# Открытие порта приложения
EXPOSE 8080

# Команда запуска приложения
ENTRYPOINT ["java", "-jar", "app.jar"]