package com.example.skillboxsixapp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.EnableKafka;

@Slf4j
@SpringBootApplication
@EnableKafka
public class OrderStatusServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderStatusServiceApplication.class, args);
	}

	@EventListener
	public void onApplicationEvent(ApplicationReadyEvent event) {
		log.info("=================== OrderStatusServiceApplication is ready to process");
	}
}
