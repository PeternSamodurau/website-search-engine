package by.spvrent.contactmanager.service;

import by.spvrent.contactmanager.model.Contact;

import java.util.List;

public interface ContactService {
    List<Contact> getAllContacts();
    void addContact(String contactString);
    void deleteContact(String email);
    void saveContactsToFile();

}
