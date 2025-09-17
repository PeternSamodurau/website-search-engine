# Практическая работа: Создание REST API для новостного сервиса

### Этап 1: Настройка проекта и окружения

*   **`build.gradle`**:
    *   `Spring Web`: для **разработки REST API с использованием Spring MVC**.
    *   `Spring Data JPA`: для работы с базой данных.
    *   `Validation`: для валидации входящих запросов.
    *   `Spring Boot Starter AOP`: для реализации аспектно-ориентированной логики.
    *   `springdoc-openapi-starter-webmvc-ui`: для **автоматической генерации интерактивной документации и UI для тестирования API (Swagger UI)**.
    *   `PostgreSQL Driver`: драйвер для подключения к нашей базе данных.
    *   `Lombok`: для сокращения шаблонного кода.
    *   `MapStruct`: для автоматического маппинга между DTO и сущностями.

*   **Инициализация данных**: Для первоначального наполнения базы данных мы будем использовать публичное API **`newsapi.org`**.
    Будет создан специальный сервис, который при старте приложения (с профилем `init`) будет запрашивать новости и сохранять их в нашу базу.

*   **`Dockerfile` и `docker-compose.yml`**: Создаем файлы для запуска приложения и базы данных PostgreSQL в Docker-контейнерах.

*   **`application.properties`**: Настраиваем подключение к базе данных, указывая URL, имя пользователя и пароль из `docker-compose.yml`.

---

### Этап 2: Слой данных (Data Layer)

* **Сущности (Entities)**: В пакете `model` создаем классы-сущности `User`, `Category`, `News`, `Comment`. Используем аннотации JPA (`@Entity`, `@Table`, `@Id`) и настраиваем связи между ними (`@ManyToOne`, `@OneToMany`).

1. User (Пользователь)

*   **Связи:**
    *   **One-to-Many** с `News`: Один пользователь может быть автором множества новостей.
    *   **One-to-Many** с `Comment`: Один пользователь может быть автором множества комментариев.

*   **Поля сущности:**
    *   `id` (`Long`): Уникальный идентификатор.
    *   `username` (`String`): Имя пользователя (будем брать из `author` в `newsapi.org`).
    *   `email` (`String`): Электронная почта.
    *   `password` (`String`): Пароль (необходим для `Spring Security`).

*   **Поля для связей:**
    *   `newsList` (`List<News>`): Список новостей, написанных этим пользователем.
    *   `comments` (`List<Comment>`): Список комментариев, оставленных этим пользователем.

---
2. Category (Категория)

*   **Связи:**
    *   **One-to-Many** с `News`: В одной категории может быть много новостей.

*   **Поля сущности:**
    *   `id` (`Long`): Уникальный идентификатор.
    *   `name` (`String`): Название категории .

*   **Поля для связей:**
    *   `newsList` (`List<News>`): Список новостей, принадлежащих этой категории.

---

3. News (Новость)

*   **Связи:**
    *   **Many-to-One** с `User`: Множество новостей могут принадлежать одному автору.
    *   **Many-to-One** с `Category`: Множество новостей могут принадлежать одной категории.
    *   **One-to-Many** с `Comment`: Одна новость может иметь множество комментариев.

*   **Поля сущности (из `newsapi.org`):**
    *   `id` (`Long`): Уникальный идентификатор.
    *   `title` (`String`): Заголовок.
    *   `description` (`String`): Краткое описание.
    *   `content` (`String`): Полное содержание.
    *   `publishedAt` (`Instant`): Дата и время публикации.
    *   `url` (`String`): Ссылка на оригинальную статью.
    *   `imageUrl` (`String`): Ссылка на изображение.

*   **Поля для связей:**
    *   `author` (`User`): Автор новости (внешний ключ `author_id`).
    *   `category` (`Category`): Категория новости (внешний ключ `category_id`).
    *   `comments` (`List<Comment>`): Список комментариев к этой новости.

---

4. Comment (Комментарий)

*   **Связи:**
    *   **Many-to-One** с `News`: Множество комментариев относятся к одной конкретной новости.
    *   **Many-to-One** с `User`: Множество комментариев могут быть написаны одним автором.

*   **Поля сущности:**
    *   `id` (`Long`): Уникальный идентификатор.
    *   `text` (`String`): Текст комментария.
    *   `createdAt` (`Instant`): Дата и время создания комментария.

*   **Поля для связей:**
    *   `author` (`User`): Автор комментария (внешний ключ `author_id`).
    *   `news` (`News`): Новость, к которой относится комментарий (внешний ключ `news_id`).

* **Репозитории**: Для каждой сущности создаем свой интерфейс-репозиторий.
*   **`resources/schema.sql`**: Создаем SQL-скрипт с командами `CREATE TABLE` для всех наших сущностей.
    Он будет автоматически выполняться при старте и создавать структуру таблиц в БД.

---
### Этап 3: Инициализация из внешнего API

1.  **Определяем точку запуска**
    *   Создаем класс-сервис компонент  `DataInitializer`,
        который реализует интерфейс `CommandLineRunner` с единственным методом `run`.
        Spring Boot выполнит разовую задачу при старте приложения, описанную в этом методе.

2.  **Выполняем запрос к внешнему API**
    *   Сформируем полный URL для запроса к `newsapi.org`.
    *   Используем HTTP-клиент (`RestTemplate`) для отправки GET-запроса по этому URL.
        и описания в каком виде получить ответы от API, класс `NewsRepository`.
    *   Используем блок `try-catch` для обработки возможных сетевых ошибок или недоступности API.

3**Добавляем проверку на наличие данных**
    *   В самом начале выполнения обращаемся к репозиторию (`NewsRepository`) для посчета количество записей в таблице.
    *   Если количество больше нуля, прерваем выполнение, чтобы избежать дублирования данных.

4.  **Преобразовываем (десериализация) ответ**
    *   Полученный от API ответ в формате JSON автоматически преобразовываем в заранее подготовленные Java-объекты (DTO).
    *   Из корневого объекта ответа извлекаем список статей.
    *   Проверяем, что список не пустой. Если он пуст, прерваем выполнение.

5.  **Подготовка данных**
    *   Берем из списка только необходимое количество статей (например, первые 10).
    *   Проверяем наличие в базе данных общей **категории** ("Tesla") - статьи про Тесла. 
        Если её нет — создаем и сохраняем. Если есть — используем существующую.

6.  **Запускаем цикл обработки и сохранения**
    *   Для каждой статьи из отобранного списка выполнияем следующие шаги:
        *   **Обработываем автора:** Проверяем, существует ли в базе `User` с таким именем. 
              Если нет — создать и сохранить нового `User`. 
              Если есть — использовать существующего.
        *   **Создаем сущность "Новость":** Создаем новый объект `News`.
        *   **Заполняем поля:** Копируем данные из статьи (заголовок, контент, дата и т.д.) в поля объекта `News`.
        *   **Устанавливаем  связи:** Присваиваем объекту `News` сущности автора и категории, полученные на предыдущих шагах.
        *   **Сохраняем в базу:** Передаем готовый объект `News` в репозиторий для сохранения (`newsRepository.save()`).

---

### Этап 4: Слой DTO (Data Transfer Objects)
 
   Этот слой определяет **формат данных для обмена** между нашим сервером и **любым внешним приложением**
   (фронтендом, мобильным приложением и т.д.).
   Он скрывает внутреннюю структуру базы данных и обеспечивает безопасность.

*   **Разделение DTO**: Проектируем два типа DTO для каждой сущности и размещаем их в отдельных папках `dto/request` и `dto/response`:
*   **Request DTO**: Классы, описывающие данные, которые **приложение отправляет на сервер** для создания или обновления сущности (например, `UserRequest`). 
      Именно в них мы размещаем аннотации валидации (`@NotBlank`, `@Size`, `@Email`) для проверки входящих данных.
*   **Response DTO**: Классы, описывающие данные, которые **сервер отправляет в ответ**. 
      Они содержат только публичную информацию (без паролей) и имеют структуру, удобную для **программной обработки**, включая ID, 
      используется самим приложением для последующих запросов на изменение или удаление этого ресурса.

---

### Этап 5: Слой mapper (MapStruct) 
    
    В файле MAPSTRUCT_EXPLANATION.md
[# Зачем нужен MapStruct](MAPSTRUCT_EXPLANATION.md)

---


### Этап 6: Слой Service на примере `NewsServiceImpl`

Он не работает напрямую с HTTP-запросами (это работа контроллера) и не выполняет SQL-запросы (это работа репозитория). 
Сервис получает данные от контроллера, запрашивает нужные сущности у репозиториев, выполняет логику и возвращает результат.

  1. Сервисы и внедрение репозиториев

Класс-сервис — это Spring-компонент, помеченный аннотацией `@Service`. 
Чтобы выполнять свою работу, ему нужны репозитории. 
Внедряем репозитории с помощью механизма **Dependency Injection**.

```java
@Service // 1.регистрирует этот класс в контексте Spring как сервис.
@RequiredArgsConstructor // 2. Lombok-аннотация для внедрения зависимостей через конструктор
@Transactional // 3. Если в середине метода произойдет ошибка, все изменения, сделанные в базе данных с начала метода, будут отменены.
public class NewsServiceImpl implements NewsService {

    // 4. Зависимости, которые будут внедрены в конструктор
    private final NewsRepository newsRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final NewsMapper newsMapper;

    // ... методы сервиса ...
}
```

 2. Фильтрация (Specification)

Это часть ТЗ. Нужно находить новости не только все подряд, но и с фильтрами (по автору, по категории).
`Specification` — это часть Spring Data JPA, 
который позволяет строить `WHERE`-условия для SQL-запроса динамически.

```java
@Override
@Transactional(readOnly = true) // Оптимизация для запросов, которые только читают данные
public Page<NewsResponse> findAll(Long authorId, Long categoryId, Pageable pageable) {
    
    // 1. Создаем объект спецификации 
    Specification<News> spec = (root, query, criteriaBuilder) -> {
        List<Predicate> predicates = new ArrayList<>(); // 2. Список наших "WHERE"-условий

        // 3. Динамически добавляем условия в список
        if (authorId != null) {
            predicates.add(criteriaBuilder.equal(root.get("author").get("id"), authorId));
        }

        if (categoryId != null) {
            predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
        }

        // 4. Объединяем все условия через 
        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };

    // 5. Выполняем запрос с созданной спецификацией и пагинацией
    return newsRepository.findAll(spec, pageable)
            .map(newsMapper::toNewsResponseForList);
}
```
   1.  Создаем реализацию интерфейса `Specification`. 
   2.  `predicates` — это список условий.
   3.  Добавляем условие в список, только если соответствующий параметр (`authorId` или `categoryId`) пришел в запросе (не равен `null`).
    Если оба параметра `null`, список останется пустым, и получим все новости.
   4.  `criteriaBuilder.and(...)` собирает все условия в одно большое `Predicate`, эквивалентное `WHERE condition1 AND condition2`.
   5.  `newsRepository.findAll(spec, pageable)` — передаем нашу спецификацию и параметры пагинации в репозиторий.
     Spring Data JPA сам генерирует и выполняет нужный SQL-запрос.

3. Контроль доступа (Применение AOP)
4. 
   Методы update и delete позволяют любому пользователю изменить или удалить любую новость. 
   Это не безопасно. С помощью AOP (Этап 7) защищаем эти методы, просто добавив аннотацию. 
   Логика проверки выносится в отдельный класс (аспект), а сервис остается чистым.
```java
@Override
@CheckEntityOwnership // <--- ПРИМЕНЯЕМ АСПЕКТ ДЛЯ ПРОВЕРКИ ПРАВ
public NewsResponse update(Long id, NewsRequest request) {
    // ... основная логика обновления новости ...
}

@Override
@CheckEntityOwnership // <--- ПРИМЕНЯЕМ АСПЕКТ ДЛЯ ПРОВЕРКИ ПРАВ
public void deleteById(Long id) {
    newsRepository.deleteById(id);
}
```
Теперь перед выполнением этих методов всегда будет происходить проверка прав доступа, 
реализованная в аспекте. 
Если проверка не пройдена, аспект выбросит исключение AccessDeniedException, 
и выполнение метода не начнется.

---
### Этап 7: AOP (Контроль доступа)

Аспектно-ориентированное программирование (AOP) позволяет вынести сквозную логику (например, безопасность, логирование) в отдельные модули — аспекты.
Используем его для проверки, что пользователь может изменять/удалять только свой собственный контент.

  1. Создание аннотации-маркера `@CheckEntityOwnership`

Это простой флаг, которым мы помечаем методы, требующие проверки. Он не содержит никакой логики.

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckEntityOwnership {
}
```

   2. Создание универсального Аспекта `OwnershipCheckAspect`

Этот аспект выполняется **перед** любым методом, помеченным нашей аннотацией `@CheckEntityOwnership`. 
Он определяет, с какой сущностью (новость или комментарий) работает, и проверяет,
совпадает ли ID ее автора с ID текущего пользователя.

```java
@Aspect
@Component
@RequiredArgsConstructor
public class OwnershipCheckAspect {

    // Внедряем все необходимые репозитории
    private final NewsRepository newsRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    @Before("@annotation(com.example.springbootnewsportal.aop.CheckEntityOwnership) && args(id, ..)")
    public void checkOwnership(JoinPoint joinPoint, Long id) {
        // 1. Получаем юзера из стандартного контекста безопасности Spring
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found in database"));

        // 2. Определяем имя сервиса (NewsServiceImpl или CommentServiceImpl)
        String targetClassName = joinPoint.getTarget().getClass().getSimpleName();

        // 3. Выполняем проверку в зависимости от типа сущности
        if (targetClassName.equals("NewsServiceImpl")) {
            News news = newsRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("News not found with ID: " + id));
            if (!news.getAuthor().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("User does not have permission to modify this resource");
            }
        } else if (targetClassName.equals("CommentServiceImpl")) {
            Comment comment = commentRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Comment not found with ID: " + id));
            if (!comment.getAuthor().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("User does not have permission to modify this resource");
            }
        } else {
            // Защита на случай, если аннотацию по ошибке повесят на другой сервис
            throw new UnsupportedOperationException("Ownership check not implemented for " + targetClassName);
        }
    }
}
```
---

### Этап 9: Исключения (Глобальная обработка)

Чтобы не возвращать клиенту стандартные неинформативные страницы ошибок, 
создаем глобальный обработчик исключений с аннотацией `@RestControllerAdvice`. 
Он перехватывает исключения, выброшенные в любой части приложения, 
и формирует стандартизированный JSON-ответ с корректным HTTP-статусом.

   1. Создание DTO для ответа `ErrorResponseDTO`

Это простой POJO-класс, который определяет структуру JSON-ответа об ошибке.

```java
@Data
@AllArgsConstructor
public class ErrorResponseDTO {
    private int status;
    private String message;
}
```

   2. Создание кастомных исключений (Пример: `ResourceNotFoundException`)

Cоздаем собственные классы исключений.

```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

   3. Создание обработчика `GlobalExceptionHandler`

Он содержит методы, каждый из которых отвечает за обработку определенного типа исключения.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // HTTP 404: Ресурс не найден
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponseDTO handleResourceNotFound(ResourceNotFoundException ex) {
        return new ErrorResponseDTO(HttpStatus.NOT_FOUND.value(), ex.getMessage());
    }

    // HTTP 403: Доступ запрещен (от нашего аспекта)
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponseDTO handleAccessDenied(AccessDeniedException ex) {
        return new ErrorResponseDTO(HttpStatus.FORBIDDEN.value(), "Access is denied. You do not have permission to perform this action.");
    }

    // HTTP 400: Ошибка валидации DTO (например, @NotBlank)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDTO handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return new ErrorResponseDTO(HttpStatus.BAD_REQUEST.value(), "Validation failed: " + errorMessage);
    }

    // HTTP 500: Все остальные непредвиденные ошибки
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponseDTO handleAllUncaughtException(Exception ex) {
        // В реальном приложении здесь обязательно должно быть логирование ошибки
        return new ErrorResponseDTO(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred. Please contact support.");
    }
}
```

