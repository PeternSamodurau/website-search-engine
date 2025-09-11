package com.example.demo.controller;

import com.example.demo.model.Contact;
import com.example.demo.service.ContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/") // Все URL в этом контроллере будут начинаться с "/"
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    public String list(Model model) {
        log.info("GET / : Request to get all contacts");
        model.addAttribute("contacts", contactService.findAll());
        return "list";
    }

    @GetMapping("/add")
    public String add(Model model) {
        log.info("GET /add : Request to show add form");
        model.addAttribute("contact", new Contact());
        return "edit-form";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable UUID id, Model model) {
        log.info("GET /edit/{} : Request to show edit form", id);
        Optional<Contact> contact = contactService.findById(id);
        if (contact.isPresent()) {
            model.addAttribute("contact", contact.get());
            return "edit-form";
        } else {
            log.warn("Contact with id {} not found for editing", id);
            return "redirect:/";
        }
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Contact contact) {

        if (contact.getId() == null) {
            log.info("POST /save : Request to save new contact: {}", contact);
            contactService.save(contact);
        } else {
            log.info("POST /save : Request to update contact: {}", contact);
            contactService.update(contact);
        }
        return "redirect:/";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable UUID id) {
        log.info("GET /delete/{} : Request to delete contact", id);
        contactService.deleteById(id);
        return "redirect:/";
    }
}