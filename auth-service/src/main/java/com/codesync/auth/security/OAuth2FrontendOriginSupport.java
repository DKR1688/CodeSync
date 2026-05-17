package com.codesync.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.util.StringUtils;

public final class OAuth2FrontendOriginSupport {

	public static final String FRONTEND_ORIGIN_SESSION_ATTRIBUTE = "codesync.oauth.frontend-origin";

	private OAuth2FrontendOriginSupport() {
	}

	public static void storeFrontendOrigin(HttpServletRequest request, String candidateOrigin) {
		String normalizedOrigin = normalizeOrigin(candidateOrigin);
		HttpSession session = request.getSession(true);

		if (normalizedOrigin == null) {
			session.removeAttribute(FRONTEND_ORIGIN_SESSION_ATTRIBUTE);
			return;
		}

		session.setAttribute(FRONTEND_ORIGIN_SESSION_ATTRIBUTE, normalizedOrigin);
	}

	public static String resolveFrontendOrigin(HttpServletRequest request, String fallbackOrigin) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			Object sessionValue = session.getAttribute(FRONTEND_ORIGIN_SESSION_ATTRIBUTE);
			if (sessionValue instanceof String storedOrigin && StringUtils.hasText(storedOrigin)) {
				session.removeAttribute(FRONTEND_ORIGIN_SESSION_ATTRIBUTE);
				String normalizedStoredOrigin = normalizeOrigin(storedOrigin);
				if (normalizedStoredOrigin != null) {
					return normalizedStoredOrigin;
				}
			}
		}

		String refererOrigin = extractOrigin(request.getHeader("Referer"));
		if (refererOrigin != null) {
			return refererOrigin;
		}

		String requestOrigin = normalizeOrigin(request.getHeader("Origin"));
		if (requestOrigin != null) {
			return requestOrigin;
		}

		return normalizeOrigin(fallbackOrigin);
	}

	public static String extractOrigin(String url) {
		if (!StringUtils.hasText(url)) {
			return null;
		}

		try {
			URI uri = new URI(url.trim());
			if (!StringUtils.hasText(uri.getScheme()) || !StringUtils.hasText(uri.getHost())) {
				return null;
			}

			return buildOrigin(uri);
		} catch (URISyntaxException ex) {
			return null;
		}
	}

	public static String normalizeOrigin(String candidateOrigin) {
		if (!StringUtils.hasText(candidateOrigin)) {
			return null;
		}

		try {
			URI uri = new URI(candidateOrigin.trim());
			String scheme = uri.getScheme();
			String host = uri.getHost();

			if (!StringUtils.hasText(scheme) || !StringUtils.hasText(host)) {
				return null;
			}

			String normalizedScheme = scheme.toLowerCase();
			if (!"http".equals(normalizedScheme) && !"https".equals(normalizedScheme)) {
				return null;
			}

			return buildOrigin(uri);
		} catch (URISyntaxException ex) {
			return null;
		}
	}

	private static String buildOrigin(URI uri) {
		StringBuilder builder = new StringBuilder();
		builder.append(uri.getScheme().toLowerCase()).append("://").append(uri.getHost());
		if (uri.getPort() != -1) {
			builder.append(':').append(uri.getPort());
		}
		return builder.toString();
	}
}
