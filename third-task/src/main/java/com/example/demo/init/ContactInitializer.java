package com.example.demo.init;

import com.example.demo.model.Contact;
import com.example.demo.repository.ContactRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@Profile("init")
@RequiredArgsConstructor
public class ContactInitializer {

    private final ContactRepository contactRepository;

    @Value("${app.init.contacts-file-path:default-contacts.txt}")
    private String contactsFilePath;

    @PostConstruct
    public void initialize() {
        log.info("Profile 'init' is active. Initializing contacts from file: {}", contactsFilePath);

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(contactsFilePath)) {
            if (is == null) {
                log.error("ERROR: File '{}' not found in classpath.", contactsFilePath);
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                long count = reader.lines()
                        .map(line -> line.split(","))
                        .filter(parts -> parts.length == 4)
                        .peek(parts -> {
                            try {
                                Contact contact = new Contact();
                                contact.setFirstName(parts[0].trim());
                                contact.setLastName(parts[1].trim());
                                contact.setEmail(parts[2].trim());
                                contact.setPhone(parts[3].trim());
                                contactRepository.save(contact);
                            } catch (Exception e) {
                                log.warn("Could not process line: '{}'", String.join(",", parts), e);
                            }
                        })
                        .count();
                log.info("Successfully loaded {} contacts.", count);
            }
        } catch (Exception e) {
            log.error("An error occurred while loading default contacts: {}", e.getMessage(), e);
        }
    }
}
