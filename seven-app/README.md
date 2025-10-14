# План разработки "Трекера задач" 

## 1: Настройка проекта и зависимостей

### Проверить build.gradle:  
    с MongoDB и MapStruct.•spring-boot-starter-webflux: 
    для реактивных контроллеров.•spring-boot-starter-data-mongodb-reactive: 
    для реактивной работы с MongoDB.•org.mapstruct:mapstruct:
    для маппинга DTO.•org.projectlombok:lombok: 
    для сокращения шаблонного кода (getters, setters, constructors).
    Также нужно настроить annotationProcessor для Lombok и MapStruct.

## 2: Конфигурация базы данных

### Настроить application.properties 
    Добавьте строку подключения к вашей базе данных MongoDB.
    Propertiesspring.data.mongodb.uri=mongodb://localhost:27017/task_tracker

## 3: Создание моделей (Entities)

### Создать пакет model 
    В src/main/java/com/example/seven_app/ создайте пакет model
    Enum TaskStatus: Создайте enum TaskStatus с полями TODO, IN_PROGRESS, DONE
    Сущность User: Создайте класс User в пакете model.
    Аннотируйте его @Document(collection = "users") для MongoDB.
    Добавьте поля: id (с аннотацией @Id), username, email.
    Используйте Lombok аннотации @Data, @NoArgsConstructor, @AllArgsConstructor для простоты.
    Сущность Task: Создайте класс Task в пакете model.
    Аннотируйте его @Document(collection = "tasks").
    Добавьте поля для хранения в БД: id (@Id), name, description, createdAt, updatedAt, status, authorId, assigneeId, observerIds.
    Добавьте поля, которые не будут храниться в БД: author, assignee, observers. Пометьте их аннотацией @ReadOnlyProperty.

## 4: Создание репозиториев

### Создать пакет repository.
    UserRepository: Создайте интерфейс UserRepository, который наследуется от ReactiveMongoRepository<User, String>. Spring Data автоматически предоставит вам CRUD-методы.
    TaskRepository: Аналогично создайте интерфейс TaskRepository, наследуемый от ReactiveMongoRepository<Task, String>.

## 5: Создание DTO и мапперов (MapStruct)

### Создать пакет dto
    Здесь будут классы для передачи данных между клиентом и сервером.
    UserDto: с полями id, username, email.
    TaskDto: с полями id, name, description и т.д., а также вложенными объектами UserDto author, UserDto assignee, Set<UserDto> observers.
    Возможно, понадобятся отдельные DTO для запросов на создание/обновление (например, CreateUserRequest).

### Создать пакет mapper
    UserMapper: Создайте интерфейс UserMapper с аннотацией @Mapper(componentModel = "spring").
    Определите методы для преобразования: User toEntity(UserDto dto), UserDto toDto(User entity).
    TaskMapper: Создайте TaskMapper. Он будет преобразовывать базовые поля задачи. Преобразование вложенных сущностей (автор, исполнитель) будет происходить в сервисном слое.

## Реализация сервисного слоя (Бизнес-логика)

### Создать пакет service
    UserService: Создайте класс UserService.
    Внедрите (@Autowired) UserRepository и UserMapper.
    Реализуйте методы: findAll(), findById(), save(), update(), deleteById(). 
    Эти методы будут вызывать репозиторий и возвращать Flux<UserDto> или Mono<UserDto>.
    TaskService: Это самый сложный и интересный класс.•Внедрите TaskRepository, UserRepository, TaskMapper, UserMapper.
    Для методов findAll() и findById():
    * 1.Получите задачу (или поток задач) из TaskRepository.
    * 2.Для каждой задачи используйте flatMap. 
    * 3.Получите Mono<User> для автора (userRepository.findById(task.getAuthorId())).
    * 4.Получите Mono<User> для исполнителя (userRepository.findById(task.getAssigneeId())).
    * 5.Получите Flux<User> для наблюдателей (userRepository.findAllById(task.getObserverIds())) и соберите их в Mono<Set<User>> с помощью .collect(Collectors.toSet()).
    * 6.Используйте Mono.zip для объединения всех этих Mono.
    * 7.Внутри zip вы получите кортеж с задачей, автором, исполнителем и наблюдателями. Установите эти объекты в поля task.setAuthor(...), task.setAssignee(...) и т.д.
    * 8.Верните обновленную задачу.•Для save() и update(): Принимайте DTO, преобразуйте в сущность, устанавливайте createdAt/updatedAt и сохраняйте.
    Для addObserver(): Получите задачу, добавьте ID нового наблюдателя в observerIds и сохраните.

##  Создание контроллеров (API Endpoints)

##  Создать пакет controller
    UserController: Создайте класс с аннотацией @RestController и @RequestMapping("/api/users").
    Внедрите UserService.
    Создайте методы для каждого эндпоинта (@GetMapping, @PostMapping и т.д.), которые будут вызывать соответствующие методы сервиса и возвращать Flux<UserDto> или Mono<UserDto>.
    TaskController: Аналогично создайте контроллер для задач с эндпоинтами, указанными в ТЗ.