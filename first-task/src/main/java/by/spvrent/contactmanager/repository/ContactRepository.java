package by.spvrent.contactmanager.repository;

import by.spvrent.contactmanager.model.Contact;

import java.util.List;

public interface ContactRepository {
    List<Contact> findAll();
    void deleteByEmail(String email);

    void addContact(Contact contact);

    Contact findByEmail(String email);

    void saveAll(List<Contact> contacts);
}
