package com.codesync.collab.client;

import com.codesync.collab.dto.CodeFileDTO;
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

import java.util.Map;

@Component
public class FileServiceClient {

	private final RestClient restClient;

	public FileServiceClient(RestClient.Builder restClientBuilder,
			@Value("${file.service.url:http://localhost:8083}") String fileServiceUrl) {
		this.restClient = restClientBuilder.baseUrl(fileServiceUrl).build();
	}

	public CodeFileDTO getFileById(Long fileId, String authorizationHeader) {
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
			throw new DownstreamServiceException(
					"File service request failed with status " + statusCode.value(),
					ex);
		} catch (ResourceAccessException ex) {
			throw new DownstreamServiceException("File service is unavailable", ex);
		} catch (RestClientException ex) {
			throw new DownstreamServiceException("File service request failed", ex);
		}
	}

	public void updateContent(Long fileId, String content, String authorizationHeader) {
		try {
			restClient.put()
					.uri("/api/v1/files/{id}/content", fileId)
					.headers(headers -> copyAuthorizationHeader(headers, authorizationHeader))
					.body(Map.of("content", content))
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientResponseException ex) {
			HttpStatusCode statusCode = ex.getStatusCode();
			if (statusCode.value() == 404) {
				throw new ResourceNotFoundException("File not found with id " + fileId);
			}
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
}
