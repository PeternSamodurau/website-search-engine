
package com.example.springbootnewsportal.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPIDescription(){
        Server localhostServer = new Server();
        localhostServer.setUrl("http://localhost:8081");
        localhostServer.setDescription("Local Server");

        Server prodactionServer = new Server();
        prodactionServer.setUrl("http://some.prod.url");
        prodactionServer.setDescription("Prodaction Server");

        Contact contact = new Contact();
        contact.setName("Peters");
        contact.setEmail("someemail@mail.ru");
        contact.setUrl("http://someContact.url");

        License license = new License();
        license.setName("My license: 1618641");
        license.setUrl("http://someLicense.url");

        Info info = new Info();
        info.title("News Portal API");
        info.description("API for managing news, comments, users, and categories.");
        info.version("1.0.0");
        info.contact(contact);
        info.license(license);

        return new OpenAPI().info(info).servers( List.of(localhostServer, prodactionServer));

    }
}
