package com.codesync.auth.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

	private final String frontendBaseUrl;

	public OAuth2LoginFailureHandler(
			@Value("${codesync.frontend-base-url:http://localhost:4200}") String frontendBaseUrl) {
		this.frontendBaseUrl = frontendBaseUrl;
	}

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {
		String resolvedFrontendOrigin = OAuth2FrontendOriginSupport.resolveFrontendOrigin(request, frontendBaseUrl);
		String redirectUrl = UriComponentsBuilder.fromUriString(resolvedFrontendOrigin)
				.path("/auth/login")
				.queryParam("oauthError", "true")
				.build(true)
				.toUriString();

		getRedirectStrategy().sendRedirect(request, response, redirectUrl);
	}
}
