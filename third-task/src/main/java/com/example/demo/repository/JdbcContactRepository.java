package com.example.demo.repository;

import com.example.demo.mapper.ContactRowMapper;
import com.example.demo.model.Contact;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
@Primary
public class JdbcContactRepository implements ContactRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ContactRowMapper contactRowMapper;

    public JdbcContactRepository(JdbcTemplate jdbcTemplate, ContactRowMapper contactRowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.contactRowMapper = contactRowMapper;
        log.info("JdbcContactRepository initialized with JdbcTemplate and ContactRowMapper.");
    }

    @Override
    public List<Contact> findAll() {
        log.info("Attempting to find all contacts.");
        String sql = "SELECT * FROM contacts";
        log.debug("Executing SQL: {}", sql);
        List<Contact> contacts = jdbcTemplate.query(sql, contactRowMapper);
        log.info("Found {} contacts.", contacts.size());
        return contacts;
    }

    @Override
    public Optional<Contact> findById(UUID id) {
        log.info("Attempting to find contact by ID: {}", id);
        String sql = "SELECT * FROM contacts WHERE id = ?";
        log.debug("Executing SQL: {} with ID: {}", sql, id);
        List<Contact> contacts = jdbcTemplate.query(sql, contactRowMapper, id);
        Optional<Contact> result = contacts.stream().findFirst();
        if (result.isPresent()) {
            log.info("Contact with ID {} found.", id);
        } else {
            log.info("Contact with ID {} not found.", id);
        }
        return result;
    }

    @Override
    public Optional<Contact> findByEmail(String email) {
        log.info("Attempting to find contact by email: {}", email);
        String sql = "SELECT * FROM contacts WHERE email = ?";
        log.debug("Executing SQL: {} with email: {}", sql, email);
        List<Contact> contacts = jdbcTemplate.query(sql, contactRowMapper, email);
        Optional<Contact> result = contacts.stream().findFirst();
        if (result.isPresent()) {
            log.info("Contact with email {} found.", email);
        } else {
            log.info("Contact with email {} not found.", email);
        }
        return result;
    }

    @Override
    public Contact save(Contact contact) {
        log.info("Attempting to save new contact: {}", contact);
        String sql = "INSERT INTO contacts (id, first_name, last_name, email, phone) VALUES (?, ?, ?, ?, ?)";
        contact.setId(UUID.randomUUID());
        log.debug("Executing SQL: {} with parameters: id={}, firstName={}, lastName={}, email={}, phone={}",
                sql, contact.getId(), contact.getFirstName(), contact.getLastName(), contact.getEmail(), contact.getPhone());
        jdbcTemplate.update(sql,
                contact.getId(),
                contact.getFirstName(),
                contact.getLastName(),
                contact.getEmail(),
                contact.getPhone());
        log.info("Contact saved successfully with ID: {}", contact.getId());
        return contact;
    }

    @Override
    public int update(Contact contact) {
        log.info("Attempting to update contact with ID: {}", contact.getId());
        String sql = "UPDATE contacts SET first_name = ?, last_name = ?, email = ?, phone = ? WHERE id = ?";
        log.debug("Executing SQL: {} with parameters: firstName={}, lastName={}, email={}, phone={}, id={}",
                sql, contact.getFirstName(), contact.getLastName(), contact.getEmail(), contact.getPhone(), contact.getId());
        int rowsAffected = jdbcTemplate.update(sql,
                contact.getFirstName(),
                contact.getLastName(),
                contact.getEmail(),
                contact.getPhone(),
                contact.getId());
        log.info("Contact with ID {} updated. Rows affected: {}", contact.getId(), rowsAffected);
        return rowsAffected;
    }

    @Override
    public int deleteById(UUID id) {
        log.info("Attempting to delete contact by ID: {}", id);
        String sql = "DELETE FROM contacts WHERE id = ?";
        log.debug("Executing SQL: {} with ID: {}", sql, id);
        int rowsAffected = jdbcTemplate.update(sql, id);
        log.info("Contact with ID {} deleted. Rows affected: {}", id, rowsAffected);
        return rowsAffected;
    }
}
