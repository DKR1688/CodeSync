package com.codesync.auth.resource;

import com.codesync.auth.security.OAuth2FrontendOriginSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;

@Controller
public class OAuth2LoginController {

	private final String gatewayBaseUrl;

	public OAuth2LoginController(@Value("${codesync.gateway-base-url:http://localhost:8080}") String gatewayBaseUrl) {
		this.gatewayBaseUrl = gatewayBaseUrl;
	}

	@GetMapping("/auth/oauth2/{provider}")
	public String authorize(@PathVariable String provider,
			@RequestParam(required = false) String frontendOrigin,
			HttpServletRequest request) {
		String normalizedProvider = provider == null ? "" : provider.trim().toLowerCase();
		if (!"google".equals(normalizedProvider) && !"github".equals(normalizedProvider)) {
			return "redirect:/auth/login?oauthError=true";
		}

		String originCandidate = frontendOrigin;
		if (originCandidate == null || originCandidate.isBlank()) {
			originCandidate = OAuth2FrontendOriginSupport.extractOrigin(request.getHeader("Referer"));
		}

		OAuth2FrontendOriginSupport.storeFrontendOrigin(request, originCandidate);
		return "redirect:" + resolveGatewayOrigin(originCandidate) + "/oauth2/authorization/" + normalizedProvider;
	}

	private String resolveGatewayOrigin(String frontendOrigin) {
		try {
			URI gatewayUri = URI.create(gatewayBaseUrl);
			String host = gatewayUri.getHost();
			if (frontendOrigin != null && !frontendOrigin.isBlank()) {
				URI frontendUri = URI.create(frontendOrigin);
				if (isLocalHost(frontendUri.getHost())) {
					host = frontendUri.getHost();
				}
			}

			String path = gatewayUri.getPath();
			String normalizedPath = path == null || path.isBlank() || "/".equals(path)
					? ""
					: path.replaceAll("/+$", "");

			StringBuilder origin = new StringBuilder();
			origin.append(gatewayUri.getScheme()).append("://").append(host);
			if (gatewayUri.getPort() != -1) {
				origin.append(':').append(gatewayUri.getPort());
			}
			origin.append(normalizedPath);
			return origin.toString();
		} catch (IllegalArgumentException ex) {
			return gatewayBaseUrl.replaceAll("/+$", "");
		}
	}

	private boolean isLocalHost(String host) {
		if (host == null) {
			return false;
		}
		String normalizedHost = host.trim().toLowerCase();
		return "localhost".equals(normalizedHost) || "127.0.0.1".equals(normalizedHost) || "::1".equals(normalizedHost);
	}
}
