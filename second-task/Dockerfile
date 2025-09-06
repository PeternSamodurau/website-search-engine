FROM openjdk:17-oracle

WORKDIR /app

COPY build/libs/skillbox-second-task-0.0.1-SNAPSHOT.jar app.jar

ENV CREATE_ON_STARTUP=true

CMD ["java", "-jar", "app.jar"]