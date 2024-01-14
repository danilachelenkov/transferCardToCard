#Задаем базовый образ для контейнер
FROM openjdk:22-ea-21-slim-bullseye

#Внутренний порт REST-приложения
EXPOSE 5500

COPY target/card-to-card-service-0.0.1.jar cardservice.jar

CMD ["java", "-jar", "cardservice.jar"]