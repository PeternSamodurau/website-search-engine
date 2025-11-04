package ru.spvrent;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;

@Slf4j
@SpringBootApplication

@OpenAPIDefinition(
		info = @Info(title = "News Portal API", version = "v1"),
		security = @SecurityRequirement(name = "basicAuth")
)
@SecurityScheme(
		name = "basicAuth",
		type = SecuritySchemeType.HTTP,
		scheme = "basic"
)

public class WebsiteSearchEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebsiteSearchEngineApplication.class, args);
	}


	@EventListener
	public void onApplicationEvent(WebServerInitializedEvent event) {
		int port = event.getWebServer().getPort();
		String url = "http://localhost:" + port;
		log.info("=================== WebsiteSearchEngineApplication started on URL : {}", url);
	}

}
