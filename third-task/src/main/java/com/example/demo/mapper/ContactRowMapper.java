package com.example.demo.mapper;

import com.example.demo.model.Contact;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@Slf4j
@Component
public class ContactRowMapper implements RowMapper<Contact> {

    @Override
    public Contact mapRow(ResultSet rs, int rowNum) throws SQLException {
        log.info("Mapping row number {} to Contact object.", rowNum);

        Contact contact = new Contact();
        // Устанавливаем поля из данных, полученных из базы
        contact.setId(rs.getObject("id", UUID.class));
        contact.setFirstName(rs.getString("first_name"));
        contact.setLastName(rs.getString("last_name"));
        contact.setEmail(rs.getString("email"));
        contact.setPhone(rs.getString("phone"));

        log.debug("Successfully mapped row {} to Contact: {}", rowNum, contact);

        return contact;
    }
}
