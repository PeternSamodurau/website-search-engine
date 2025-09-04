package by.spvrent.contactmanager.repository;

import by.spvrent.contactmanager.model.Contact;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryContactRepository implements ContactRepository {

    private final Map<String, Contact> contacts = new ConcurrentHashMap<>();

    @Override
    public void saveAll(List<Contact> contactList) {
        contactList.forEach(contact -> contacts.put(contact.getEmail(), contact));
    }

    @Override
    public List<Contact> findAll() {
        return List.copyOf(contacts.values());
    }

    @Override
    public void deleteByEmail(String email) {
        contacts.remove(email);
    }

    @Override
    public void addContact(Contact contact) {
        contacts.put(contact.getEmail(), contact);
    }

    @Override
    public Contact findByEmail(String email) {
        return contacts.get(email);
    }
}
