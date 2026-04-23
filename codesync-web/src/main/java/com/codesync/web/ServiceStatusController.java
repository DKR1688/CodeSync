package com.codesync.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServiceStatusController {

	@GetMapping("/")
	public Map<String, Object> root() {
		return Map.of(
				"service", "codesync-web",
				"type", "headless-web-service",
				"frontend", "managed in separate Angular repository",
				"health", "/actuator/health");
	}
}
