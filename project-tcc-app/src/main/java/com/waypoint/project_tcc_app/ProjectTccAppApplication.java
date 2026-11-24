package com.waypoint.project_tcc_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ProjectTccAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProjectTccAppApplication.class, args);
	}

}
