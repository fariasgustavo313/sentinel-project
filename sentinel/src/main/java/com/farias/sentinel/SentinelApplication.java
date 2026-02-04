package com.farias.sentinel;

import com.farias.sentinel.service.ContainerService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SentinelApplication {

	public static void main(String[] args) {
		SpringApplication.run(SentinelApplication.class, args);
	}

	@Bean
	public CommandLineRunner run(ContainerService containerService) {
		return args -> {
			// esto se ejecuta justo despues de que arranca la app
			containerService.monitorearContenedores();
		};
	}

}
