package com.codesync.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@ActiveProfiles("prod")
@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"eureka.client.enabled=false",
				"spring.cloud.discovery.enabled=false"
		})
class ApiGatewayProdProfileTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void prodProfileLoadsGatewayRoutesWithValidPredicates() {
		WebTestClient.bindToApplicationContext(applicationContext)
				.build()
				.get()
				.uri("/actuator/gateway/routes")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.length()").isEqualTo(8)
				.jsonPath("$[?(@.route_id=='auth-service')].uri")
				.isEqualTo("https://codesync-auth-service.onrender.com:443")
				.jsonPath("$[?(@.route_id=='file-service')].uri")
				.isEqualTo("https://codesync-file-service.onrender.com:443")
				.jsonPath("$[?(@.route_id=='execution-service')].uri")
				.isEqualTo("https://codesync-execution-service.onrender.com:443");
	}
}
