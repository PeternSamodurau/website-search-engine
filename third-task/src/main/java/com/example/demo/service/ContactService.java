package com.example.demo.service;

import com.example.demo.model.Contact;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContactService {
    List<Contact> findAll();
    Optional<Contact> findById(UUID id);
    Contact save(Contact contact);
    Contact update(Contact contact);
    void deleteById(UUID id);
}
