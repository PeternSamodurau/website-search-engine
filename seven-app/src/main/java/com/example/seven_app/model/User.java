package com.example.seven_app.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users") //Это параметр, который уточняет, в какую именно коллекцию нужно сохранять объекты User.
public class User {

    @Id
    private String id;
    private String username;
    private String email;

}
