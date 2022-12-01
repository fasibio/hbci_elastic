package de.fasibio.hbciapp;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class HbciAppApplication {

	public static void main(String[] args) {
		// new
		new SpringApplicationBuilder(HbciAppApplication.class).web(WebApplicationType.NONE).run(args);
		// System.exit(SpringApplication.exit(SpringApplication.run(HbciAppApplication.class,
		// args)));
	}

}
