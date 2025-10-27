# Практическая работа: Трекер задач (Task Tracker) with Spring Security

Приложение для отслеживания задач, разработанное на Spring WebFlux.

## Запуск приложения

1. Выполните команду:
   
 **.\docker\docker-start.cmd**

2. Выполняется инициализация:

   * AuthorInitializer.java из app.default-author-username=userAuthor и  app.default-author-usermail=userAuthor@mail.ru
   * UserInitializer.java 
   * TaskInitializer.java

3. Откройте Swagger в браузере http://localhost:8080/swagger-ui.html

# Тестирование API через Swagger UI

Для тестирования необходимо авторизоваться в Swagger UI, нажав на кнопку "Authorize" и введя логин и пароль одного из тестовых пользователей.

### Тестовые пользователи

| Логин               | Пароль      | Роли                        |
|:--------------------|:------------|:----------------------------|
| `user1@example.com` | `password1` | `ROLE_USER`, `ROLE_MANAGER` |
| `user2@example.com` | `password2` | `ROLE_USER`                 |
| `user3@example.com` | `password3` | `ROLE_USER`                 |
| `user4@example.com` | `password4` | `ROLE_USER`                 |
| `user5@example.com` | `password5` | `ROLE_USER`                 |

---