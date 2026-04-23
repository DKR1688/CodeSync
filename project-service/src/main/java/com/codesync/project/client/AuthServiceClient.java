package com.codesync.project.client;

import com.codesync.project.exception.DownstreamServiceException;
import com.codesync.project.exception.InvalidProjectRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class AuthServiceClient {

	private final RestClient discoveryRestClient;
	private final RestClient directRestClient;

	public AuthServiceClient(@LoadBalanced RestClient.Builder loadBalancedRestClientBuilder,
			RestClient.Builder restClientBuilder,
			@Value("${auth.service.url:http://localhost:8081}") String authServiceUrl) {
		this.discoveryRestClient = loadBalancedRestClientBuilder.baseUrl("http://AUTH-SERVICE").build();
		this.directRestClient = restClientBuilder.baseUrl(authServiceUrl).build();
	}

	public void assertUserExists(Long userId) {
		try {
			assertUserExists(discoveryRestClient, userId);
			return;
		} catch (IllegalStateException ex) {
			// Discovery not ready, fall back to the configured direct URL.
		}
		assertUserExists(directRestClient, userId);
	}

	private void assertUserExists(RestClient restClient, Long userId) {
		try {
			UserLookupResponse response = restClient.get()
					.uri("/auth/profile/{id}", userId)
					.retrieve()
					.body(UserLookupResponse.class);
			if (response == null || response.getUserId() <= 0) {
				throw new DownstreamServiceException("Auth service returned an invalid user response");
			}
		} catch (RestClientResponseException ex) {
			HttpStatusCode statusCode = ex.getStatusCode();
			if (statusCode.value() == 400 || statusCode.value() == 404) {
				throw new InvalidProjectRequestException("User not found with id " + userId);
			}
			throw new DownstreamServiceException(
					"Auth service request failed with status " + statusCode.value(),
					ex);
		} catch (ResourceAccessException ex) {
			throw new DownstreamServiceException("Auth service is unavailable", ex);
		} catch (RestClientException ex) {
			throw new DownstreamServiceException("Auth service request failed", ex);
		}
	}

	static class UserLookupResponse {
		private int userId;

		public int getUserId() {
			return userId;
		}

		public void setUserId(int userId) {
			this.userId = userId;
		}
	}
}
