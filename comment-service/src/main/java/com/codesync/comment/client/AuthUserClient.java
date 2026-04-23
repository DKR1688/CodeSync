package com.codesync.comment.client;

import com.codesync.comment.dto.UserSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

@Component
public class AuthUserClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuthUserClient.class);

	private final RestClient discoveryRestClient;
	private final RestClient directRestClient;

	public AuthUserClient(@LoadBalanced RestClient.Builder loadBalancedRestClientBuilder,
			RestClient.Builder restClientBuilder,
			@Value("${comment.client.auth-service-url:http://localhost:8081}") String authServiceUrl) {
		this.discoveryRestClient = loadBalancedRestClientBuilder.baseUrl("http://AUTH-SERVICE").build();
		this.directRestClient = restClientBuilder.baseUrl(authServiceUrl).build();
	}

	public Optional<UserSummary> findActiveUserByUsername(String username, String authorizationHeader) {
		if (!StringUtils.hasText(username)) {
			return Optional.empty();
		}
		try {
			return findActiveUserByUsername(discoveryRestClient, username, authorizationHeader);
		} catch (IllegalStateException ex) {
			return findActiveUserByUsername(directRestClient, username, authorizationHeader);
		}
	}

	private Optional<UserSummary> findActiveUserByUsername(RestClient restClient, String username,
			String authorizationHeader) {
		try {
			List<UserSummary> users = restClient.get()
					.uri(uriBuilder -> uriBuilder.path("/auth/search").queryParam("username", username).build())
					.headers(headers -> copyAuthorizationHeader(headers, authorizationHeader))
					.retrieve()
					.body(new ParameterizedTypeReference<>() {
					});
			if (users == null) {
				return Optional.empty();
			}
			return users.stream()
					.filter(UserSummary::isActive)
					.filter(user -> username.equalsIgnoreCase(user.getUsername()))
					.findFirst();
		} catch (RestClientException ex) {
			LOGGER.warn("Unable to resolve mentioned user '{}': {}", username, ex.getMessage());
			return Optional.empty();
		} catch (RuntimeException ex) {
			LOGGER.warn("Unable to resolve mentioned user '{}': {}", username, ex.getMessage());
			return Optional.empty();
		}
	}

	private void copyAuthorizationHeader(HttpHeaders headers, String authorizationHeader) {
		if (StringUtils.hasText(authorizationHeader)) {
			headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
		}
	}
}
