package by.spvrent.contactmanager.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@Profile("init")
public class ContactInitializer {

    private final ContactService contactService;

    @Autowired
    public ContactInitializer(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostConstruct
    public void initialize() {
        System.out.println("Загрузка контактов по умолчанию...");

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("default-contacts.txt")) {
            if (is == null) {
                log.error("ОШИБКА: Файл 'default-contacts.txt' не найден в ресурсах.");
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                reader.lines().forEach(contactService::addContact);
            }
            System.out.println("Контакты по умолчанию успешно загружены.");
        } catch (Exception e) {
            log.error("Произошла ошибка при загрузке контактов по умолчанию: " + e.getMessage());
        }
    }
}
