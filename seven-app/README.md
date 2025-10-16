# Практическая работа: Трекер задач (Task Tracker)

Приложение для отслеживания задач, разработанное на Spring WebFlux.

## Технологии

*   **Java 17**
*   **Spring Boot 3** (с использованием WebFlux для реактивного программирования)
*   **MongoDB** (в качестве базы данных)
*   **Docker** и **Docker Compose** (для контейнеризации и запуска)
*   **Springdoc OpenAPI** (для документации API)

## Предварительные требования

*   Установленный [Docker Desktop](https://www.docker.com/products/docker-desktop/).

## Запуск приложения

1. Выполните команду:
   
 **.\docker\docker-start.cmd**

2. Выполняется инициализация:

   * AuthorInitializer.java из app.default-author-username=userAuthor и  app.default-author-usermail=userAuthor@mail.ru
   * UserInitializer.java 
   * TaskInitializer.java

3. Откройте Swagger в браузере http://localhost:8080/swagger-ui.html

4. <strong><font color="red">ВНИМАНИЕ!!!</font></strong> тестируйте andpoints DELETE во всех контроллерах последними.
   Иначе Swagger UI продолжит показывать в выпадающих списках ID, которых уже не будет существовать в базе данных.
