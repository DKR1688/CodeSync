package com.codesync.collab.client;

import com.codesync.collab.dto.ProjectPermissionDTO;
import com.codesync.collab.exception.DownstreamServiceException;
import com.codesync.collab.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ProjectPermissionClient {

	private final RestClient restClient;

	public ProjectPermissionClient(RestClient.Builder restClientBuilder,
			@Value("${project.service.url:http://localhost:8082}") String projectServiceUrl) {
		this.restClient = restClientBuilder.baseUrl(projectServiceUrl).build();
	}

	public ProjectPermissionDTO getPermissions(Long projectId, String authorizationHeader) {
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
			throw new DownstreamServiceException(
					"Project service request failed with status " + statusCode.value(),
					ex);
		} catch (ResourceAccessException ex) {
			throw new DownstreamServiceException("Project service is unavailable", ex);
		} catch (RestClientException ex) {
			throw new DownstreamServiceException("Project service request failed", ex);
		}
	}

	private void copyAuthorizationHeader(HttpHeaders headers, String authorizationHeader) {
		if (StringUtils.hasText(authorizationHeader)) {
			headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
		}
	}
}
