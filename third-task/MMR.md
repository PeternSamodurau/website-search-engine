# Создание связки Модель-Маппер-Репозиторий с `JdbcTemplate`

Задача: как преобразовать данные из реляционных таблиц (строки и колонки) в объекты Java (объекты и поля).

Связка **Модель-Маппер-Репозиторий** — это паттерн, который решает эту задачу. 

*   **Модель (`Model`)** — это "контейнер" для данных. Простой Java-класс, поля которого соответствуют колонкам в таблице.
*   **Репозиторий (`Repository`)** — это "исполнитель". Он отвечает за выполнение SQL-запросов к базе данных.
*   **Маппер (`Mapper`)** — это "карта пути связи". Он содержит инструкцию, как именно сопоставить данные из одной строки SQL-результата с полями одного объекта Модели.

## Шаг 1: Модель (`Contact.java`) — Контейнер для данных

Создаем простой Java-класс (POJO), который представляет сущность в приложении. 
Его поля должны соответствовать колонкам в таблице базы данных.

public class Contact {

private UUID id;
private String firstName;
private String lastName;
private String email;
private String phone;

}

## Шаг 2: Маппер (`ContactRowMapper.java`) — Переводчик

Реализует интерфейс Spring `RowMapper<T>`, где `T` — это тип нашей модели (`Contact`). 
Переопределяем его единственный метод `mapRow`, который будет вызываться для **каждой** строки, полученной из базы данных.

Берем строку из `ResultSet` (результат SQL-запроса на одну строку) и перекладываем данные из него в новый объект `Contact`.

@Component // Делаем Маппер Spring-компонентом, чтобы его можно было внедрять
public class ContactRowMapper implements RowMapper<Contact> {

    @Override
    public Contact mapRow(ResultSet rs, int rowNum) throws SQLException {
        // 1. Создаем пустой объект-контейнер
        Contact contact = new Contact();

        // 2. Извлекаем данные из ResultSet по именам колонок
        //    и устанавливаем их в поля нашего объекта.
        //    Имена колонок ("id", "first_name") должны точно
        //    совпадать с именами в таблице БД.
        contact.setId(rs.getObject("id", UUID.class));
        contact.setFirstName(rs.getString("first_name"));
        contact.setLastName(rs.getString("last_name"));
        contact.setEmail(rs.getString("email"));
        contact.setPhone(rs.getString("phone"));

        // 3. Возвращаем полностью заполненный объект
        return contact;
    }
}

## Шаг 3: Репозиторий (`JdbcContactRepository.java`) — Исполнитель

Репозиторий выполняет SQL-запросы. Он не знает, как создавать объекты `Contact`, но он знает, где взять "переводчика" — `ContactRowMapper`. 
Внедряем маппер в репозиторий через конструктор, используя Dependency Injection.

**Задача:** Выполнить SQL-запрос и передать `JdbcTemplate` инструкцию (Маппер) о том, что делать с результатами.

Когда вызывается метод jdbcTemplate.query(sql, contactRowMapper), внутри Spring `JdbcTemplate` происходит следующий процесс:

1.  **Получение соединения:** `JdbcTemplate` получает готовое соединение с базой данных.

2.  **Создание `Statement`:** Создается объект `PreparedStatement` на основе `sql`-строки.

3.  **Выполнение запроса:** `JdbcTemplate` выполняет запрос и получает от драйвера базы данных объект `ResultSet`. Представьте `ResultSet` как курсор, стоящий *перед* первой строкой результата.

4.  **Итерация:**
    *   `JdbcTemplate` внутри себя запускает цикл: `while (resultSet.next())`.
    *   Для каждой строки, полученной от БД `JdbcTemplate` вызывает метод `mapRow()` из  `contactRowMapper`.
    *   В этот метод `mapRow(resultSet, rowNum)` передаетм текущую строку (`resultSet`) и ее номер.
    *   `ContactRowMapper` выполняет свою единственную задачу: создает `new Contact()` и заполняет его поля данными из `resultSet`.
    *   `mapRow()` возвращает полностью готовый объект `Contact` обратно `JdbcTemplate`.

5.  **Сборка результата:** `JdbcTemplate` берет каждый объект `Contact`, который ему вернул маппер `ContactRowMapper`, и складывает его во внутренний `List`.
  
6. **Возврат результата:** `JdbcTemplate` возвращает полностью готовый и заполненный `List<Contact>`.




@Repository
public class JdbcContactRepository implements ContactRepository {

    private final JdbcTemplate jdbcTemplate; // инструкция
    private final ContactRowMapper contactRowMapper; // Наш "переводчик"
    
    public JdbcContactRepository(JdbcTemplate jdbcTemplate, ContactRowMapper contactRowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.contactRowMapper = contactRowMapper;
    }

    @Override
    public List<Contact> findAll() {
        String sql = "SELECT * FROM contacts";
       
        // просим jdbcTemplate выполнить запрос и для каждой строки результата использовать наш contactRowMapper для создания объекта.
        return jdbcTemplate.query(sql, contactRowMapper);
 }

### Схематичное представление

[Репозиторий]
|
1. Вызывает jdbcTemplate.query(SQL, Маппер)
   |
   V
   [JdbcTemplate]
   |
2. Получает строки из БД
3. Для каждой строки вызывает Маппер.mapRow(строка)
   |
   V
   [Маппер]
   |
4. Создает и возвращает объект [Модель]
   |
   V
   [Модель]

Этот паттерн позволяет достичь чистого разделения ответственности:
Репозиторий отвечает за **что** делать (какой SQL выполнить), 
а Маппер — за **как** преобразовать результат. 
Модель просто служит структурой для хранения этих данных в приложении.