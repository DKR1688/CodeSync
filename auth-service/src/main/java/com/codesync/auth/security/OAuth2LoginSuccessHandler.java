package com.codesync.auth.security;

import com.codesync.auth.entity.User;
import com.codesync.auth.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final AuthService authService;
	private final String frontendBaseUrl;

	public OAuth2LoginSuccessHandler(AuthService authService,
			@Value("${codesync.frontend-base-url:http://127.0.0.1:4200}") String frontendBaseUrl) {
		this.authService = authService;
		this.frontendBaseUrl = frontendBaseUrl;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
		Map<String, Object> attributes = oauth2User.getAttributes();

		String email = (String) attributes.get("email");
		String name = (String) attributes.get("name");
		String username = (String) attributes.get("login");
		if (username == null && email != null) {
			username = email.substring(0, email.indexOf('@'));
		}

		String provider = request.getRequestURI().contains("github") ? "GITHUB" : "GOOGLE";
		User user = authService.upsertOAuthUser(email, username, name, provider);
		String token = authService.issueToken(user);

		String redirectUrl = frontendBaseUrl + "/auth/callback?provider=" + provider.toLowerCase()
				+ "&success=true&token=" + token;
		getRedirectStrategy().sendRedirect(request, response, redirectUrl);
	}
}
