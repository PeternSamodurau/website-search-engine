package com.example.demo.repository;

import com.example.demo.model.Contact;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContactRepository {
    List<Contact> findAll();
    Optional<Contact> findById(UUID id);
    Optional<Contact> findByEmail(String email);
    Contact save(Contact contact);
    int update(Contact contact);
    int deleteById(UUID id);
}
