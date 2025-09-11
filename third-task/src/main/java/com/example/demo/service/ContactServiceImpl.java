package com.example.demo.service;

import com.example.demo.exception.ContactNotFoundException;
import com.example.demo.exception.EmailAlreadyExistsException;
import com.example.demo.model.Contact;
import com.example.demo.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;

    @Override
    public List<Contact> findAll() {
        log.info("Finding all contacts in service.");
        return contactRepository.findAll();
    }

    @Override
    public Optional<Contact> findById(UUID id) {
        log.info("Finding contact by ID: {} in service.", id);
        return contactRepository.findById(id);
    }

    @Override
    public Contact save(Contact contact) {
        log.info("Attempting to save new contact in service: {}", contact);
        // Проверка на уникальность email
        contactRepository.findByEmail(contact.getEmail()).ifPresent(existingContact -> {
            log.warn("Attempt to save contact with duplicate email: {}", contact.getEmail());
            throw new EmailAlreadyExistsException("Contact with email " + contact.getEmail() + " already exists.");
        });
        log.debug("Email {} is unique. Proceeding to save.", contact.getEmail());
        return contactRepository.save(contact);
    }

    @Override
    public Contact update(Contact contact) {
        log.info("Attempting to update contact in service: {}", contact);
        // Проверка, что контакт с таким ID существует
        contactRepository.findById(contact.getId()).orElseThrow(() -> {
            log.warn("Attempt to update non-existent contact with ID: {}", contact.getId());
            return new ContactNotFoundException("Contact with ID " + contact.getId() + " not found.");
        });
        log.debug("Contact with ID {} exists. Proceeding to update.", contact.getId());
        contactRepository.update(contact);
        return contact;
    }

    @Override
    public void deleteById(UUID id) {
        log.info("Attempting to delete contact by ID: {} in service.", id);
        // Проверка, что контакт с таким ID существует
        contactRepository.findById(id).orElseThrow(() -> {
            log.warn("Attempt to delete non-existent contact with ID: {}", id);
            return new ContactNotFoundException("Contact with ID " + id + " not found.");
        });
        log.debug("Contact with ID {} exists. Proceeding to delete.", id);
        contactRepository.deleteById(id);
    }
}
