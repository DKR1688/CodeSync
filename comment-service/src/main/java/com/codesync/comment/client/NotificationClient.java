package com.codesync.comment.client;

import com.codesync.comment.dto.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class NotificationClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(NotificationClient.class);

	private final RestClient restClient;

	public NotificationClient(@LoadBalanced RestClient.Builder restClientBuilder) {
		this.restClient = restClientBuilder.baseUrl("http://NOTIFICATION-SERVICE").build();
	}

	public void send(NotificationRequest request, String authorizationHeader) {
		try {
			restClient.post()
					.uri("/api/v1/notifications")
					.headers(headers -> copyAuthorizationHeader(headers, authorizationHeader))
					.body(request)
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientException ex) {
			LOGGER.warn("Unable to dispatch comment notification for recipient {}: {}",
					request != null ? request.getRecipientId() : null, ex.getMessage());
		}
	}

	private void copyAuthorizationHeader(HttpHeaders headers, String authorizationHeader) {
		if (StringUtils.hasText(authorizationHeader)) {
			headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
		}
	}
}
