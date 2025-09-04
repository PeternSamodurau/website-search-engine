package by.spvrent.contactmanager.service;

import by.spvrent.contactmanager.model.Contact;
import by.spvrent.contactmanager.repository.ContactRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;
    private final String storagePath;

    public ContactServiceImpl(ContactRepository contactRepository,
                              @Value("${app.storage.path}") String storagePath) {
        this.contactRepository = contactRepository;
        this.storagePath = storagePath;
    }

    @Override
    public List<Contact> getAllContacts() {
        return contactRepository.findAll();
    }

    @Override
    public void addContact(String contactString) {
        String[] parts = contactString.split(";");
        if (parts.length == 3) {
            String fullName = parts[0].trim();
            String phoneNumber = parts[1].trim();
            String email = parts[2].trim();

            if (contactRepository.findByEmail(email) != null) {
                log.warn("Попытка добавить контакт с уже существующим email: {}", email);
                System.err.println("Ошибка: Контакт с таким email уже существует.");
                return;
            }

            Contact contact = new Contact(fullName, phoneNumber, email);
            contactRepository.addContact(contact);
            log.info("Новый контакт успешно добавлен: {}", fullName);
            System.out.println("Контакт успешно добавлен.");
        } else {
            log.error("Неверный формат ввода контакта: {}. Ожидалось 3 части, разделенные ';'", contactString);
            System.err.println("Ошибка: Неверный формат ввода. Ожидается: Ф. И. О.; номер телефона; email");
        }
    }

    @Override
    public void deleteContact(String email) {
        contactRepository.deleteByEmail(email);
        log.info("Контакт с email '{}' удален (если существовал).", email);
        System.out.println("Контакт с email '" + email + "' удален (если существовал).");
    }

    @Override
    public void saveContactsToFile() {
        List<Contact> contacts = contactRepository.findAll();
        List<String> lines = contacts.stream()
                .map(contact -> String.join(";", contact.getFullName(), contact.getPhoneNumber(), contact.getEmail()))
                .collect(Collectors.toList());

        try {
            Path filePath = Paths.get(storagePath);
            Files.write(filePath, lines);
            log.info("Контакты ({} шт.) успешно сохранены в файл: {}", contacts.size(), storagePath);
            System.out.println("Контакты успешно сохранены в файл: " + storagePath);
        } catch (IOException e) {
            log.error("Ошибка при сохранении контактов в файл '{}'", storagePath, e);
            System.err.println("Ошибка при сохранении контактов в файл: " + e.getMessage());
        }
    }
}
