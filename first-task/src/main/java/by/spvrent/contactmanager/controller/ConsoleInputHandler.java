package by.spvrent.contactmanager.controller;

import by.spvrent.contactmanager.model.Contact;
import by.spvrent.contactmanager.service.ContactService;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Scanner;

@Component
public class ConsoleInputHandler {

    private final ContactService contactService;

    public ConsoleInputHandler(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostConstruct
    public void startConsoleListener() {
        new Thread(() -> {
            printHelp();
            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.print("> ");
                    String line = scanner.nextLine();
                    if (!processCommand(line)) {
                        break;
                    }
                }
            }
            System.out.println("Приложение завершено.");
            System.exit(0);
        }).start();
    }

    private boolean processCommand(String line) {
        if (line.trim().isEmpty()) {
            return true;
        }
        String[] parts = line.split("\s+", 2);
        String command = parts[0].toLowerCase();

        try {
            switch (command) {
                case "exit":
                    return false;
                case "list":
                    printAllContacts();
                    break;
                case "add":
                    if (parts.length > 1) {
                        contactService.addContact(parts[1]);
                    } else {
                        System.err.println("Ошибка: для команды 'add' нужны данные. Пример: add ФИО;Телефон;Email");
                    }
                    break;
                case "delete":
                    if (parts.length > 1) {
                        contactService.deleteContact(parts[1]);
                    } else {
                        System.err.println("Ошибка: для команды 'delete' нужен email. Пример: delete user@example.com");
                    }
                    break;
                case "save":
                    contactService.saveContactsToFile();
                    break;
                case "help":
                    printHelp();
                    break;
                default:
                    System.err.println("Неизвестная команда: '" + command + "'. Введите 'help' для списка команд.");
                    break;
            }
        } catch (Exception e) {
            System.err.println("Произошла ошибка: " + e.getMessage());
        }
        return true;
    }

    private void printAllContacts() {
        List<Contact> contacts = contactService.getAllContacts();
        if (contacts.isEmpty()) {
            System.out.println("Список контактов пуст.");
        } else {
            System.out.println("--- Список контактов ---");
            contacts.forEach(c -> System.out.printf("%s | %s | %s%n", c.getFullName(), c.getPhoneNumber(), c.getEmail()));
            System.out.println("----------------------");
        }
    }

    private void printHelp() {
        String helpMessage = """
                --- Доступные команды ---
                list - Показать все контакты
                add <ФИО;Телефон;Email> - Добавить новый контакт
                delete <Email> - Удалить контакт по email
                save - Сохранить все контакты в файл
                help - Показать это сообщение
                exit - Выйти из приложения
                -----------------------
                """;
        System.out.println(helpMessage);
    }
}
