package com.codesync.file.client;

import com.codesync.file.dto.CreateSnapshotRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class VersionServiceClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(VersionServiceClient.class);

	private final RestClient discoveryRestClient;
	private final RestClient directRestClient;

	public VersionServiceClient(@LoadBalanced RestClient.Builder loadBalancedRestClientBuilder,
			RestClient.Builder restClientBuilder,
			@Value("${file.client.version-service-url:http://localhost:8085}") String versionServiceUrl) {
		this.discoveryRestClient = loadBalancedRestClientBuilder.baseUrl("http://VERSION-SERVICE").build();
		this.directRestClient = restClientBuilder.baseUrl(versionServiceUrl).build();
	}

	public void createSnapshot(CreateSnapshotRequest request, String authorizationHeader) {
		try {
			createSnapshot(discoveryRestClient, request, authorizationHeader);
			return;
		} catch (IllegalStateException ex) {
			// Discovery not ready, fall back to the configured direct URL.
		}
		createSnapshot(directRestClient, request, authorizationHeader);
	}

	private void createSnapshot(RestClient restClient, CreateSnapshotRequest request, String authorizationHeader) {
		try {
			restClient.post()
					.uri("/api/v1/versions")
					.headers(headers -> copyAuthorizationHeader(headers, authorizationHeader))
					.body(request)
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientException ex) {
			LOGGER.warn("Unable to create direct version snapshot for file {}: {}",
					request != null ? request.getFileId() : null, ex.getMessage());
		} catch (RuntimeException ex) {
			LOGGER.warn("Unable to create direct version snapshot for file {}: {}",
					request != null ? request.getFileId() : null, ex.getMessage());
		}
	}

	private void copyAuthorizationHeader(HttpHeaders headers, String authorizationHeader) {
		if (StringUtils.hasText(authorizationHeader)) {
			headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
		}
	}
}
