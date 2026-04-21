package com.codesync.version.client;

import com.codesync.version.dto.FileContentUpdateRequest;
import com.codesync.version.exception.DownstreamServiceException;
import com.codesync.version.exception.ResourceNotFoundException;
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

	public void updateFileContent(Long fileId, String content, String authorizationHeader) {
		try {
			restClient.put()
					.uri("/api/v1/files/{id}/content", fileId)
					.headers(headers -> copyAuthorizationHeader(headers, authorizationHeader))
					.body(new FileContentUpdateRequest(content))
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientResponseException ex) {
			HttpStatusCode statusCode = ex.getStatusCode();
			if (statusCode.value() == 404) {
				throw new ResourceNotFoundException("File not found with id " + fileId);
			}
			throw new DownstreamServiceException("File service request failed with status " + statusCode.value(), ex);
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
}
