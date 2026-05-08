package com.codesync.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"eureka.client.enabled=false",
				"spring.cloud.discovery.enabled=false",
				"AUTH_SERVICE_URL=https://legacy-auth.example.com",
				"PROJECT_SERVICE_URL=https://legacy-project.example.com"
		})
class ApiGatewayRoutePropertiesTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void legacyServiceUrlPropertiesDoNotOverrideGatewayRoutes() {
		WebTestClient.bindToApplicationContext(applicationContext)
				.build()
				.get()
				.uri("/actuator/gateway/routes")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$[?(@.route_id=='auth-service')].uri").isEqualTo("lb://AUTH-SERVICE")
				.jsonPath("$[?(@.route_id=='project-service')].uri").isEqualTo("lb://PROJECT-SERVICE");
	}

}
