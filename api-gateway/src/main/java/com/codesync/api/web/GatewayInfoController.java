package com.codesync.api.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GatewayInfoController {

	private static final Map<String, String> SERVICE_ROUTES = Map.of(
			"authService", "/auth/**, /oauth2/**, /login/oauth2/**",
			"projectService", "/api/v1/projects/**",
			"fileService", "/api/v1/files/**");

	@GetMapping({ "/", "/gateway/info" })
	public Map<String, Object> gatewayInfo() {
		return Map.of(
				"name", "CodeSync API Gateway",
				"status", "UP",
				"serviceRoutes", SERVICE_ROUTES,
				"healthEndpoint", "/actuator/health",
				"gatewayRoutesEndpoint", "/actuator/gateway/routes");
	}
}
