# Практическая работа: CRUD-приложение для управления книгами

### Запуск приложения

1. Выполните **.\gradlew clean build.**
2. Выполните **docker-compose -f docker/docker-compose.yml up --build | Tee-Object -FilePath "docker/docker-log.txt"**
3. Можно открыть Swagger в браузере http://localhost:8081/swagger-ui/index.html
   Страница загрузится без пароля.

### Настройка проекта и окружения

*   **`build.gradle`**:
    *   `Spring Web`: для **разработки REST API с использованием Spring MVC**.
    *   `Spring Data JPA`: для работы с базой данных.
    *   `Validation`: для валидации входящих запросов.
    *   `Spring Boot Starter AOP`: для реализации аспектно-ориентированной логики.
    *   `springdoc-openapi-starter-webmvc-ui`: для **автоматической генерации интерактивной документации и UI для тестирования API (Swagger UI)**.
    *   `PostgreSQL Driver`: драйвер для подключения к нашей базе данных.
    *   `Lombok`: для сокращения шаблонного кода.
    *   `MapStruct`: для автоматического маппинга между DTO и сущностями.

### Инициализация данных из Google Books API с помощю FeignClients

*   **Инициализация данных**: Для первоначального наполнения базы данных мы будем использовать данные из **Google Books API**.
    
    - **Spring** начинает искать интерфейсы, которые помеченны аннотацией @FeignClient.
    
    - **FeignClient** читает аннотации из интерфейса **GoogleBooksClient**. 
    
    - Для метода **searchBooks** он видит @GetMapping("/volumes") и два параметра с @RequestParam.
    
    - Внутри сгенерированного метода searchBooks он размещает код, который: 
       - Берет базовый URL из аннотации @FeignClient ( который, в свою очередь, берется из application.yml). 
       - Добавляет к нему путь /volumes. 
       - Берет аргументы, переданные в метод (query и maxResults), и формирует из них строку запроса
         https://www.googleapis.com/books/v1/volumes?q=a&maxResults=40 
       - Использует встроенный HTTP-клиент для отправки этого собранного GET-запроса по сети. 
       - Получает ответ в виде JSON. 
       - Использует **Jackson** для преобразования этого JSON в Java -объект GoogleBooksSearchResponse. 
       - Возвращает этот объект.
    
    - **Spring** регистрирует **GoogleBooksClient** в своем " контейнере" (Application Context) как бин.
        Теперь для Spring существует готовый к работе объект, который реализует интерфейс GoogleBooksClient.
    
    Создается **DataInitializer** специальный сервис, который при старте приложения (с профилем `init`) будет запрашивать книги и категории.
    Для его создания и нужен **GoogleBooksClient**.
    Загружаются категории и книги в базу.


### Схема работы приложения

[Схема работы приложения](Creating%20a%20REST%20API%20for%20a%20news%20service.drawio)