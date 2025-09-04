package by.spvrent.contactmanager.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ComponentScan("by.spvrent.contactmanager")
@PropertySource("classpath:application.properties")
public class AppConfig {
}
