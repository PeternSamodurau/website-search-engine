# Зачем нужен MapStruct

В нашем приложении существуют два "представления" одних и тех же данных:

### 1. Сущности (Entities)
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

### 2. DTO (Data Transfer Objects)
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

# Разбор аннотаций в `NewsMapper.java`

### 1. Аннотация на уровне интерфейса

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

### 2. Методы и их аннотации

#### Метод `toNews`

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

#### Метод `toNewsResponseWithComments`

```java
@Mapping(source = "author.username", target = "authorUsername")
@Mapping(source = "category.categoryName", target = "categoryName")
@Mapping(target = "commentsCount", expression = "java(news.getComments() != null ? (long) news.getComments().size() : 0L)")
NewsResponse toNewsResponseWithComments(News news);
```

*   **Назначение**: Преобразование сущности `News` из базы данных в "полный" DTO `NewsResponse`, который будет отправлен клиенту.
*   **`@Mapping(source = "author.username", target = "authorUsername")`**: 
    Берем исходный объект `news`, из него получи поле `author`, из объекта `author` получи поле `username` и пишем результат в поле `authorUsername`  объекта `NewsResponse`". 
    MapStruct сгенерирует код.
*   **`@Mapping(source = "category.categoryName", target = "categoryName")`**: Аналогично предыдущему, для категории.
*   **`@Mapping(target = "commentsCount", expression = "...")`**: 
    Это маппинг на основе выражения. Для поля `commentsCount` в `NewsResponse` не ищи соответствующее поле в `News`. 
    Вместо этого **выполни указанный Java-код**". Выражение `java(news.getComments() != null ? (long) news.getComments().size() : 0L)` 
    безопасно вычисляет размер списка комментариев.

#### Метод `toNewsResponseForList`

```java
// ... аннотации для authorUsername, categoryName, commentsCount ...
@Mapping(target = "comments", ignore = true)
NewsResponse toNewsResponseForList(News news);
```

*   **Создание DTO для использования в списках**.
*   **`@Mapping(target = "comments", ignore = true)`**: 
    При выполнении этого маппинга, **полностью проигнорируй поле `comments`**". 
    Это делается для оптимизации: когда клиент запрашивает список из 20 новостей, 
    ему не нужны полные списки комментариев для каждой из них. 
    Мы отдаем только их количество (`commentsCount`), а сам список — нет.

#### Метод `updateNewsFromRequest`

```java
@Mapping(target = "id", ignore = true)
@Mapping(target = "author", ignore = true)
@Mapping(target = "category", ignore = true)
void updateNewsFromRequest(NewsRequest request, @MappingTarget News news);
```

*   **Обновление полей существующей сущности `News` данными из `NewsRequest`**.
*   **`@MappingTarget`**: Она говорит MapStruct: "**Не создавай новый объект `News`**. 
    Вместо этого возьми тот объект, который передан в этом параметре, и обнови его поля значениями из `request`". 
    Это позволяет сохранить `id` и другие связанные данные сущности, которую мы предварительно загрузили из базы.
*   **`@Mapping(target = "id", ignore = true)`**: Запрещает обновление первичного ключа. 
    Это важно для безопасности и целостности данных.
*   **`@Mapping(target = "author", ignore = true)`** и **`@Mapping(target = "category", ignore = true)`**: 
    Запрещают смену автора или категории через этот метод. 
   

  