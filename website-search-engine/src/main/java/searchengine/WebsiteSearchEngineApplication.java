package searchengine;

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

public class WebsiteSearchEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebsiteSearchEngineApplication.class, args);
	}


	@EventListener
	public void onApplicationEvent(WebServerInitializedEvent event) {
		int port = event.getWebServer().getPort();
		String host = "http://localhost:";
		log.info("==================================================================");
		log.info("Application is ready! You can access it at the following URLs:");
		log.info("Home page:     " + host + port + "/");
		log.info("Swagger UI:    " + host + port + "/swagger-ui.html");
		log.info("==================================================================");
	}

}
