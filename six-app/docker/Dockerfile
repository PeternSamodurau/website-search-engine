FROM openjdk:17-oracle

ARG SERVICE_NAME

WORKDIR /app

COPY ${SERVICE_NAME}/build/libs/*.jar app.jar

CMD ["java", "-jar", "app.jar"]
