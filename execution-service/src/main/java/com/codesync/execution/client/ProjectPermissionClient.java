package com.codesync.execution.client;

import com.codesync.execution.dto.ProjectPermissionDTO;
import com.codesync.execution.exception.DownstreamServiceException;
import com.codesync.execution.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class ProjectPermissionClient {

	private static final String DISCOVERY_BASE_URL = "http://PROJECT-SERVICE";
	private static final String RENDER_FALLBACK_BASE_URL = "https://codesync-project-service.onrender.com";

	private final List<RestClient> restClients;

	public ProjectPermissionClient(@LoadBalanced RestClient.Builder loadBalancedRestClientBuilder,
			RestClient.Builder restClientBuilder,
			@Value("${codesync.services.project.base-url:http://localhost:8082}") String projectServiceBaseUrl,
			@Value("${PORT:}") String deployedPort,
			@Value("${codesync.http.connect-timeout-seconds:3}") int connectTimeoutSeconds,
			@Value("${codesync.http.read-timeout-seconds:8}") int readTimeoutSeconds) {
		this.restClients = new ArrayList<>();
		this.restClients.add(buildClient(loadBalancedRestClientBuilder, DISCOVERY_BASE_URL,
				connectTimeoutSeconds, readTimeoutSeconds));
		this.restClients.add(buildClient(restClientBuilder, projectServiceBaseUrl,
				connectTimeoutSeconds, readTimeoutSeconds));
		if (shouldAddRenderFallback(projectServiceBaseUrl, deployedPort)) {
			this.restClients.add(buildClient(restClientBuilder, RENDER_FALLBACK_BASE_URL,
					connectTimeoutSeconds, readTimeoutSeconds));
		}
	}

	public ProjectPermissionDTO getPermissions(Long projectId, String authorizationHeader) {
		EndpointUnavailableException lastFailure = null;
		for (RestClient restClient : restClients) {
			try {
				return requestPermissions(restClient, projectId, authorizationHeader);
			} catch (EndpointUnavailableException ex) {
				lastFailure = ex;
			}
		}
		if (lastFailure != null) {
			throw new DownstreamServiceException("Project service is unavailable", lastFailure);
		}
		throw new DownstreamServiceException("Project service is unavailable");
	}

	private ProjectPermissionDTO requestPermissions(RestClient restClient, Long projectId, String authorizationHeader) {
		try {
			ProjectPermissionDTO response = restClient.get()
					.uri("/api/v1/projects/{id}/permissions", projectId)
					.headers(headers -> copyAuthorizationHeader(headers, authorizationHeader))
					.retrieve()
					.body(ProjectPermissionDTO.class);

			if (response == null || response.getProjectId() == null) {
				throw new DownstreamServiceException("Project service returned an invalid permission response");
			}
			return response;
		} catch (RestClientResponseException ex) {
			HttpStatusCode statusCode = ex.getStatusCode();
			if (statusCode.value() == 404) {
				throw new ResourceNotFoundException("Project not found with id " + projectId);
			}
			if (statusCode.is4xxClientError()) {
				throw new DownstreamServiceException(
						"Project service request failed with status " + statusCode.value(),
						ex);
			}
			throw new EndpointUnavailableException(
					"Project service request failed with status " + statusCode.value(),
					ex);
		} catch (ResourceAccessException ex) {
			throw new EndpointUnavailableException("Project service is unavailable", ex);
		} catch (RestClientException ex) {
			throw new EndpointUnavailableException("Project service request failed", ex);
		}
	}

	private RestClient buildClient(RestClient.Builder builder, String baseUrl, int connectTimeoutSeconds,
			int readTimeoutSeconds) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(Math.max(1, connectTimeoutSeconds)));
		requestFactory.setReadTimeout(Duration.ofSeconds(Math.max(1, readTimeoutSeconds)));
		return builder.requestFactory(requestFactory).baseUrl(baseUrl).build();
	}

	private boolean shouldAddRenderFallback(String configuredUrl, String deployedPort) {
		return StringUtils.hasText(deployedPort)
				&& !"8088".equals(deployedPort)
				&& isLocalhostUrl(configuredUrl);
	}

	private boolean isLocalhostUrl(String url) {
		if (!StringUtils.hasText(url)) {
			return true;
		}
		String normalized = url.trim().toLowerCase();
		return normalized.contains("localhost") || normalized.contains("127.0.0.1");
	}

	private void copyAuthorizationHeader(HttpHeaders headers, String authorizationHeader) {
		if (StringUtils.hasText(authorizationHeader)) {
			headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
		}
	}

	private static final class EndpointUnavailableException extends RuntimeException {

		private EndpointUnavailableException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
