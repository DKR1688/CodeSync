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
				"spring.cloud.discovery.enabled=false"
		})
class ApiGatewayApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void contextLoads() {
	}

	@Test
	void gatewayInfoEndpointExposesRegisteredServiceRoutes() {
		WebTestClient.bindToApplicationContext(applicationContext)
				.build()
				.get()
				.uri("/gateway/info")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.name").isEqualTo("CodeSync API Gateway")
				.jsonPath("$.serviceRoutes.authService").isEqualTo("/auth/**, /oauth2/**, /login/oauth2/**")
				.jsonPath("$.serviceRoutes.projectService").isEqualTo("/api/v1/projects/**")
				.jsonPath("$.serviceRoutes.fileService").isEqualTo("/api/v1/files/**");
	}

	@Test
	void gatewayActuatorEndpointIsExposedForRouteInspection() {
		WebTestClient.bindToApplicationContext(applicationContext)
				.build()
				.get()
				.uri("/actuator/gateway/routes")
				.exchange()
				.expectStatus().isOk();
	}

}
