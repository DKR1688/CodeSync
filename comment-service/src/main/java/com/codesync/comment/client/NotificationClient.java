package com.codesync.comment.client;

import com.codesync.comment.dto.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class NotificationClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(NotificationClient.class);

	private final RestClient restClient;

	public NotificationClient(@Qualifier("restClientBuilder") RestClient.Builder restClientBuilder,
			@Value("${comment.client.notification-service-url:http://localhost:8087}") String notificationServiceUrl) {
		this.restClient = restClientBuilder.baseUrl(notificationServiceUrl).build();
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
		} catch (RuntimeException ex) {
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
