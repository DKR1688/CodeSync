package com.codesync.project.client;

import com.codesync.project.exception.DownstreamServiceException;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class FileServiceClient {

	private final RestClient restClient;

	public FileServiceClient(@LoadBalanced RestClient.Builder restClientBuilder) {
		this.restClient = restClientBuilder.baseUrl("http://FILE-SERVICE").build();
	}

	public void copyProjectFiles(Long sourceProjectId, Long targetProjectId, String authorizationHeader) {
		try {
			restClient.post()
					.uri("/api/v1/files/projects/copy")
					.headers(headers -> copyAuthorizationHeader(headers, authorizationHeader))
					.body(new CopyProjectFilesRequest(sourceProjectId, targetProjectId))
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientResponseException ex) {
			HttpStatusCode statusCode = ex.getStatusCode();
			throw new DownstreamServiceException(
					"File service request failed with status " + statusCode.value(),
					ex);
		} catch (ResourceAccessException ex) {
			throw new DownstreamServiceException("File service is unavailable", ex);
		} catch (RestClientException ex) {
			throw new DownstreamServiceException("File service request failed", ex);
		}
	}

	private void copyAuthorizationHeader(HttpHeaders headers, String authorizationHeader) {
		if (StringUtils.hasText(authorizationHeader)) {
			headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
		}
	}

	private record CopyProjectFilesRequest(Long sourceProjectId, Long targetProjectId) {
	}
}
