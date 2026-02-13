package de.pamunda.nexusfin.exchange.ex_world_engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ExWorldEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExWorldEngineApplication.class, args);
	}

}
