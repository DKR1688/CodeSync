package com.codesync.collab.client;

import com.codesync.collab.dto.CodeFileDTO;
import com.codesync.collab.exception.DownstreamServiceException;
import com.codesync.collab.exception.ResourceNotFoundException;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class FileServiceClient {

	private static final String DISCOVERY_BASE_URL = "http://FILE-SERVICE";
	private static final String RENDER_FALLBACK_BASE_URL = "https://codesync-file-service.onrender.com";

	private final List<RestClient> restClients;
	private final Set<String> configuredBaseUrls;

	public FileServiceClient(@LoadBalanced RestClient.Builder loadBalancedRestClientBuilder,
			RestClient.Builder restClientBuilder,
			@Value("${file.service.url:http://localhost:8083}") String fileServiceUrl,
			@Value("${PORT:}") String deployedPort,
			@Value("${codesync.http.connect-timeout-seconds:3}") int connectTimeoutSeconds,
			@Value("${codesync.http.read-timeout-seconds:8}") int readTimeoutSeconds) {
		this.restClients = new ArrayList<>();
		this.configuredBaseUrls = new HashSet<>();
		if (shouldPreferDirectClient(fileServiceUrl, deployedPort)) {
			addClient(restClientBuilder, fileServiceUrl, connectTimeoutSeconds, readTimeoutSeconds);
			addRenderFallbackIfNeeded(restClientBuilder, fileServiceUrl, deployedPort, connectTimeoutSeconds,
					readTimeoutSeconds);
			addClient(loadBalancedRestClientBuilder, DISCOVERY_BASE_URL, connectTimeoutSeconds, readTimeoutSeconds);
			return;
		}

		addClient(loadBalancedRestClientBuilder, DISCOVERY_BASE_URL, connectTimeoutSeconds, readTimeoutSeconds);
		addClient(restClientBuilder, fileServiceUrl, connectTimeoutSeconds, readTimeoutSeconds);
		addRenderFallbackIfNeeded(restClientBuilder, fileServiceUrl, deployedPort, connectTimeoutSeconds,
				readTimeoutSeconds);
	}

	public CodeFileDTO getFileById(Long fileId, String authorizationHeader) {
		EndpointUnavailableException lastFailure = null;
		for (RestClient restClient : restClients) {
			try {
				return getFileById(restClient, fileId, authorizationHeader);
			} catch (EndpointUnavailableException ex) {
				lastFailure = ex;
			}
		}
		if (lastFailure != null) {
			throw new DownstreamServiceException("File service is unavailable", lastFailure);
		}
		throw new DownstreamServiceException("File service is unavailable");
	}

	private CodeFileDTO getFileById(RestClient restClient, Long fileId, String authorizationHeader) {
		try {
			CodeFileDTO response = restClient.get()
					.uri("/api/v1/files/{id}", fileId)
					.headers(headers -> copyAuthorizationHeader(headers, authorizationHeader))
					.retrieve()
					.body(CodeFileDTO.class);

			if (response == null || response.getFileId() == null) {
				throw new DownstreamServiceException("File service returned an invalid file response");
			}
			return response;
		} catch (RestClientResponseException ex) {
			HttpStatusCode statusCode = ex.getStatusCode();
			if (statusCode.value() == 404) {
				throw new ResourceNotFoundException("File not found with id " + fileId);
			}
			if (statusCode.is4xxClientError()) {
				throw new DownstreamServiceException(
						"File service request failed with status " + statusCode.value(),
						ex);
			}
			throw new EndpointUnavailableException(
					"File service request failed with status " + statusCode.value(),
					ex);
		} catch (ResourceAccessException ex) {
			throw new EndpointUnavailableException("File service is unavailable", ex);
		} catch (RestClientException ex) {
			throw new EndpointUnavailableException("File service request failed", ex);
		}
	}

	public void updateContent(Long fileId, String content, String authorizationHeader) {
		EndpointUnavailableException lastFailure = null;
		for (RestClient restClient : restClients) {
			try {
				updateContent(restClient, fileId, content, authorizationHeader);
				return;
			} catch (EndpointUnavailableException ex) {
				lastFailure = ex;
			}
		}
		if (lastFailure != null) {
			throw new DownstreamServiceException("File service is unavailable", lastFailure);
		}
		throw new DownstreamServiceException("File service is unavailable");
	}

	private void updateContent(RestClient restClient, Long fileId, String content, String authorizationHeader) {
		try {
			restClient.put()
					.uri("/api/v1/files/{id}/content", fileId)
					.headers(headers -> copyAuthorizationHeader(headers, authorizationHeader))
					.body(Map.of("content", content, "liveCollaboration", true))
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientResponseException ex) {
			HttpStatusCode statusCode = ex.getStatusCode();
			if (statusCode.value() == 404) {
				throw new ResourceNotFoundException("File not found with id " + fileId);
			}
			if (statusCode.is4xxClientError()) {
				throw new DownstreamServiceException(
						"File service request failed with status " + statusCode.value(),
						ex);
			}
			throw new EndpointUnavailableException(
					"File service request failed with status " + statusCode.value(),
					ex);
		} catch (ResourceAccessException ex) {
			throw new EndpointUnavailableException("File service is unavailable", ex);
		} catch (RestClientException ex) {
			throw new EndpointUnavailableException("File service request failed", ex);
		}
	}

	private RestClient buildClient(RestClient.Builder builder, String baseUrl, int connectTimeoutSeconds,
			int readTimeoutSeconds) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(Math.max(1, connectTimeoutSeconds)));
		requestFactory.setReadTimeout(Duration.ofSeconds(Math.max(1, readTimeoutSeconds)));
		return builder.requestFactory(requestFactory).baseUrl(baseUrl).build();
	}

	private void addClient(RestClient.Builder builder, String baseUrl, int connectTimeoutSeconds,
			int readTimeoutSeconds) {
		if (!StringUtils.hasText(baseUrl) || containsBaseUrl(baseUrl)) {
			return;
		}
		configuredBaseUrls.add(baseUrl.trim());
		restClients.add(buildClient(builder, baseUrl, connectTimeoutSeconds, readTimeoutSeconds));
	}

	private void addRenderFallbackIfNeeded(RestClient.Builder builder, String configuredUrl, String deployedPort,
			int connectTimeoutSeconds, int readTimeoutSeconds) {
		if (!shouldAddRenderFallback(configuredUrl, deployedPort)) {
			return;
		}
		addClient(builder, RENDER_FALLBACK_BASE_URL, connectTimeoutSeconds, readTimeoutSeconds);
	}

	private boolean shouldAddRenderFallback(String configuredUrl, String deployedPort) {
		return StringUtils.hasText(deployedPort)
				&& !"8084".equals(deployedPort)
				&& isLocalhostUrl(configuredUrl);
	}

	private boolean shouldPreferDirectClient(String configuredUrl, String deployedPort) {
		return StringUtils.hasText(deployedPort) && StringUtils.hasText(configuredUrl) && !isLocalhostUrl(configuredUrl);
	}

	private boolean isLocalhostUrl(String url) {
		if (!StringUtils.hasText(url)) {
			return true;
		}
		String normalized = url.trim().toLowerCase();
		return normalized.contains("localhost") || normalized.contains("127.0.0.1");
	}

	private boolean containsBaseUrl(String baseUrl) {
		return configuredBaseUrls.contains(baseUrl.trim());
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
