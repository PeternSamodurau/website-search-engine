# Практическая работа: Создание REST API для новостного сервиса

### Этап 0: Запуск приложения

1. Выполните **.\gradlew clean build.**
2. Выполните **docker-compose -f docker/docker-compose.yml up --build | Tee-Object -FilePath "docker/docker-log.txt"**
3. Откройте Swagger в браузере http://localhost:8081/swagger-ui/index.html
   Страница загрузится без пароля.


### Оглавление
- [Этап 1: Настройка проекта и окружения](#этап-1-настройка-проекта-и-окружения)
- [Этап 2: Слой данных (Data Layer)](#этап-2-слой-данных-data-layer)
- [Этап 3: Инициализация из внешнего API](#этап-3-инициализация-из-внешнего-api)
- [Этап 4: Слой DTO (Data Transfer Objects)](#этап-4-слой-dto-data-transfer-objects)
- [Этап 5: Слой mapper (MapStruct)](#этап-5-слой-mapper-mapstruct)
- [Этап 6: Слой Service на примере NewsServiceImpl](#этап-6-слой-service-на-примере-newsserviceimpl)
- [Этап 7: AOP (Контроль доступа)](#этап-7-aop-контроль-доступа)
- [Этап 8: Слой Controller на примере NewsController](#этап-8-слой-controller-на-примере-newscontroller)
- [Этап 9: Исключения (Глобальная обработка)](#этап-9-исключения-глобальная-обработка)

---

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

[К оглавлению](#оглавление)

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

[К оглавлению](#оглавление)

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

[К оглавлению](#оглавление)

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

[К оглавлению](#оглавление)

---

### Этап 5: Слой mapper (MapStruct)

Зачем нужен MapStruct
В нашем приложении существуют два "представления" одних и тех же данных:

1. Сущности (Entities)
*   **Это классы** `News`, `User`, `Category` `Comment`, помеченные аннотацией `@Entity`.
*   **Их структура точно отражает структуру таблиц в базе данных. Они содержат служебные аннотации** (`@Id`, `@ManyToOne`, `@JoinColumn`),
    имеют сложные, двунаправленные связи: User содержит список своих новостей (List<News>),
    а каждая News в свою очередь ссылается на своего автора (User).
    Это создает циклическую зависимость, которая приводит к ошибке при прямой сериализации в JSON.
    Кроме того, для оптимизации используется ленивая загрузка (fetch = FetchType.LAZY),
    при которой списки (например, новости пользователя) не загружаются из базы данных до прямого обращения к ним.
*   **Проблема:** Их **нельзя отдавать наружу "как есть"**.
    Если отправить объект `User` в JSON-ответе, передасться хеш пароля, внутренний ID, список всех его новостей и комментариев,
    что создаст огромную нагрузку и проблему в безопасности.

2. DTO (Data Transfer Objects)
*   **Это простые классы-"контейнеры", которые мы описали** `NewsResponse`, `UserRequest`, `CategoryResponse`.
*   **Их структура точно отражает то, что нужно получить или отправить**.
    Они содержат только нужные поля и ничего лишнего.
    Например, `NewsResponse` содержит `authorUsername`, но не весь объект `User`.
    `NewsRequest` содержит `categoryId`, но не весь объект `Category`.

    Задача MapStruct: преобразовывать данные из одного в другой.

*   Когда приходит запрос на создание новости, нужно превратить `NewsRequest` (DTO) в `News` (Entity), чтобы сохранить в базу.
*   Когда запрашивается новость, нужно превратить `News` (Entity) из базы в `NewsResponse` (DTO), чтобы отправить в ответе.

    Без специальных инструментов единственный способ — писать код преобразования вручную в сервисах.

    Именно для полного искоренения этой ручной работы и нужна библиотека-маппер MapStruct.

    Не пишем реализацию маппера.

    Процесс разделен на два независимых этапа:

* 1.Этап Сборки: MapStruct (как annotationProcessor) запускается,
  находит интерфейс NewsMapper и генерирует исходный код NewsMapperImpl.java.
  В этот файл он добавляет аннотацию @Component, потому что указали в @Mapper атрибут componentModel = "spring".
  На этом работа MapStruct закончена.

* 2.Этап Запуска Приложения: Spring-приложение стартует.
  Spring запускает свой механизм сканирования компонентов.
  Он находит класс NewsMapperImpl, видит на нем аннотацию @Component и
  именно Spring (а не MapStruct) создает экземпляр этого класса (new NewsMapperImpl())
  и помещает его в свой контекст как бин.

* Итог: MapStruct — это генератор кода, который работает во время компиляции.
  Он пишет .java файлы по заданным правилам правилам.
  Spring — это фреймворк, который работает во время выполнения.
  Он находит сгенерированные MapStruct-ом классы (уже скомпилированные в .class)
  и создает из них бины для использования в приложении.


Разбор аннотаций в `NewsMapper.java`

1. Аннотация на уровне интерфейса

```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = CommentMapper.class)
public interface NewsMapper {
    // ...
}
```

*   **`@Mapper`**: Это главная аннотация, которая помечает интерфейс.
    Она говорит: Найди этот интерфейс и сгенерируй для него `NewsMapperImpl` с реализацией.
*   **`componentModel = "spring"`**: Это инструкция для генератора кода Spring.
    Она говорит: "Когда будешь создавать класс `NewsMapperImpl`, добавь к нему аннотацию `Component`".
    Благодаря этому, Spring при старте приложения автоматически найдет этот сгенерированный класс, создаст его экземпляр и сделает его бином,
    который можно будет внедрять в другие сервисы через `@Autowired`.
*   **`unmappedTargetPolicy = ReportingPolicy.IGNORE`**: Эта политика определяет, как MapStruct должен реагировать,
    если в целевом объекте (например, `NewsResponse`) есть поля, для которых не нашлось соответствующих полей в исходном объекте (`News`).
    `IGNORE` означает: "Просто проигнорируй эти поля и не выдавай предупреждение при компиляции". Это делает маппер более гибким.
*   **`uses = CommentMapper.class`**: функция, позволяющая комбинировать мапперы.
    Она говорит: "Когда внутри `News` тебе встретится поле `List<Comment>`, и его нужно будет преобразовать в `List<CommentResponse>`,
    **используй для этого `CommentMapper`**".
    MapStruct автоматически найдет нужный метод в `CommentMapper` и применит его к каждому элементу списка.

2. Методы и их аннотации

Метод `toNews`

```java
@Mapping(target = "author", ignore = true)
@Mapping(target = "category", ignore = true)
News toNews(NewsRequest request);
```

*   **Назначение**: Преобразование объекта `NewsRequest` (DTO, который приходит от клиента) в сущность `News`
    (которую нужно сохранить в базу).
*   **`@Mapping(target = "author", ignore = true)`**: Инструкция для генератора.
    При создании объекта `News` из `NewsRequest`, **полностью проигнорируй поле `author`**".
    Это необходимо, потому что в `NewsRequest` у нас есть только `authorId` (число),
    а в сущности `News` должно быть поле типа `User` (полноценный объект).
    MapStruct не может сам загрузить `User` из базы по `id`.
    Эта логика будет реализована в сервисном слое вручную, а мапперу мы явно запрещаем трогать это поле.
*   **`@Mapping(target = "category", ignore = true)`**: Та же самая логика, что и для поля `author`.

Метод `toNewsResponseWithComments`

```java
@Mapping(source = "author.username", target = "authorUsername")
@Mapping(source = "category.categoryName", target = "categoryName")
@Mapping(target = "commentsCount", expression = "java(news.getComments() != null ? (long) news.getComments().size() : 0L)")
NewsResponse toNewsResponseWithComments(News news);
```

*   **Назначение**: Преобразование сущности `News` из базы данных в "полный" DTO `NewsResponse`, который будет отправлен клиенту (например, при запросе одной новости по ID).
*   **`@Mapping(source = "author.username", target = "authorUsername")`**: Берем исходный объект `news`, из него получаем поле `author`, из объекта `author` получаем поле `username` и пишем результат в поле `authorUsername` объекта `NewsResponse`.
*   **`@Mapping(source = "category.categoryName", target = "categoryName")`**: Аналогично предыдущему, для категории.
*   **`@Mapping(target = "commentsCount", expression = "...")`**:
    Это маппинг на основе выражения. Для поля `commentsCount` в `NewsResponse` не ищи соответствующее поле в `News`. Вместо этого **выполни указанный Java-код**. Выражение `java(news.getComments() != null ? (long) news.getComments().size() : 0L)` безопасно вычисляет размер списка комментариев.

Методы для работы со списками (`toNewsResponseForList`)


**1. Метод-хелпер для одного элемента списка:**
```java
@Named("toNewsResponseForList")
@Mapping(source = "author.username", target = "authorUsername")
@Mapping(source = "category.categoryName", target = "categoryName")
@Mapping(target = "commentsCount", expression = "java(news.getComments() != null ? (long) news.getComments().size() : 0L)")
@Mapping(target = "comments", ignore = true)
NewsResponse toNewsResponseForList(News news);
```

*   **Назначение**: Создание DTO для **одного элемента списка**.
    Этот метод очень похож на `toNewsResponseWithComments`, но с одним ключевым отличием.
*   **`@Mapping(target = "comments", ignore = true)`**: При выполнении этого маппинга,
    полностью игнорируем поле `comments`**.
    Это делается для оптимизации: когда клиент запрашивает список из 10 новостей,
    ему не нужны полные списки комментариев для каждой из них.
    Отдаем только их количество (`commentsCount`), а сам список — нет.
*   **`@Named("toNewsResponseForList")`**: Эта аннотация дает методу **уникальное имя** "toNewsResponseForList".
    Это имя нужно, чтобы MapStruct мог отличить этот метод от `toNewsResponseWithComments`,
    так как оба они принимают `News` и возвращают `NewsResponse`.

**2. Метод для преобразования всего списка:**
```java
@IterableMapping(qualifiedByName = "toNewsResponseForList")
List<NewsResponse> toNewsResponseForList(List<News> newsList);
```

*   **Назначение**: Преобразование `List<News>` в `List<NewsResponse>`.
*   **`@IterableMapping(qualifiedByName = "toNewsResponseForList")`**:
    Когда будем преобразовывать этот список, для **каждого элемента** использется метод,
    который помечен именем **`toNewsResponseForList`**".
    Это явно указывает, какой из двух возможных мапперов
    (`toNewsResponseWithComments` или `toNewsResponseForList(News news)`) нужно использовать,
    и устраняет ошибку компиляции.

Метод `updateNewsFromRequest`

```java
@Mapping(target = "id", ignore = true)
@Mapping(target = "author", ignore = true)
@Mapping(target = "category", ignore = true)
@Mapping(target = "comments", ignore = true)
void updateNewsFromRequest(NewsRequest request, @MappingTarget News news);
```

*   **Назначение**: Обновляет поле сущности `News` данными из `NewsRequest`.
*   **`@MappingTarget`**: Она говорит MapStruct: "**Не создавай новый объект `News`**.
    Вместо этого возьми тот объект, который передан в этом параметре, и обнови его поля значениями из `request`".
    Это позволяет сохранить `id` и другие связанные данные сущности, которую мы предварительно загрузили из базы.
*   **`@Mapping(target = "...", ignore = true)`**: Запрещает обновление ключевых полей:
    *   `id`: Первичный ключ никогда не должен меняться.
    *   `author`: Автор новости не может быть изменен.
    *   `category`: Логика смены категории может быть сложнее, поэтому ее лучше вынести в сервис.
    *   `comments`: Список комментариев не должен управляться через запрос на обновление новости.

[К оглавлению](#оглавление)

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

[К оглавлению](#оглавление)

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
[К оглавлению](#оглавление)

---

### Этап 8: Слой Controller на примере `NewsController`

Его задача — принимать HTTP-запросы от клиента,
вызывать соответствующие методы сервисного слоя и возвращать клиенту ответ в формате JSON.
В контроллерах не должно быть никакой бизнес-логики.

**1. Код `NewsController.java`**
```java

@RestController // 1. Объявляет класс как REST-контроллер
@RequestMapping("/api/news") // 2. Задает базовый URL для всех методов
@RequiredArgsConstructor // 3. Внедряет NewsService через конструктор
public class NewsController {

    private final NewsService newsService;

    // GET /api/news?authorId=1&page=0&size=10
    @GetMapping
    public ResponseEntity<Page<NewsResponse>> getAllNews(
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) Long categoryId,
            Pageable pageable) { // 4. Принимает параметры для фильтрации и пагинации
        return ResponseEntity.ok(newsService.findAll(authorId, categoryId, pageable));
    }

    // GET /api/news/5
    @GetMapping("/{id}")
    public ResponseEntity<NewsResponse> getNewsById(@PathVariable Long id) { // 5. Извлекает id из URL
        return ResponseEntity.ok(newsService.findById(id));
    }

    // POST /api/news
    @PostMapping
    public ResponseEntity<NewsResponse> createNews(@RequestBody @Valid NewsRequest request) { // 6. Принимает JSON и валидирует его
        NewsResponse createdNews = newsService.create(request);
        return new ResponseEntity<>(createdNews, HttpStatus.CREATED); // 7. Возвращает статус 201 CREATED
    }

    // PUT /api/news/5
    @PutMapping("/{id}")
    public ResponseEntity<NewsResponse> updateNews(@PathVariable Long id, @RequestBody @Valid NewsRequest request) {
        return ResponseEntity.ok(newsService.update(id, request));
    }

    // DELETE /api/news/5
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNews(@PathVariable Long id) {
        newsService.deleteById(id);
        return ResponseEntity.noContent().build(); // 8. Возвращает статус 204 NO CONTENT
    }
}
```

**2. Разбор аннотаций и параметров**

1.  **`@RestController`**: Специализированная версия `@Controller`.
    Она говорит Spring, что этот класс обрабатывает HTTP-запросы, а возвращаемые им значения должны быть автоматически преобразованы в JSON и записаны в тело ответа.
2.  **`@RequestMapping("/api/news")`**: Устанавливает общий префикс для всех URL-адресов в этом контроллере.
3.  **`@RequiredArgsConstructor`**: Аннотация Lombok, которая генерирует конструктор для всех `final`-полей.
    Это современный способ внедрения зависимостей (в данном случае, `NewsService`).
4.  **`@RequestParam` и `Pageable`**: `@RequestParam` извлекает параметры из строки запроса (например, `?authorId=1`). `required = false` означает, что параметр необязателен.
    `Pageable` — это специальный объект Spring, который автоматически собирает параметры `page`, `size` и `sort` для пагинации.
5.  **`@PathVariable`**: Извлекает значение из переменной части URL (например, `id` из `/api/news/{id}`).
6.  **`@RequestBody` говорит Spring взять тело HTTP-запроса (JSON) и преобразовать его в объект `NewsRequest`.
    `@Valid` — ключевая аннотация, которая запускает процесс валидации для этого объекта на основе аннотаций, которые мы указали в самом DTO (`@NotBlank`, `@Size` и т.д.). Если валидация не пройдена, наш `GlobalExceptionHandler` вернет ошибку 400**.
7.  **`ResponseEntity`**: Это специальный класс-обертка, который позволяет полностью контролировать HTTP-ответ.
    Можем указать не только тело ответа (DTO), но и HTTP-статус.
    Например, при успешном создании ресурса принято возвращать статус `201 CREATED`.
8.  **`ResponseEntity.noContent().build()`**: При успешном удалении ресурса тело ответа должно быть пустым, а статус — `204 No Content`.

[К оглавлению](#оглавление)

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
[К оглавлению](#оглавление)
