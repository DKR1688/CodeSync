package com.codesync.auth.security;

import com.codesync.auth.entity.User;
import com.codesync.auth.repository.UserRepository;
import com.codesync.auth.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final UserRepository userRepository;

    public OAuth2LoginSuccessHandler(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oauth2User.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String username = (String) attributes.get("login"); // GitHub
        if (username == null) {
            username = email.split("@")[0]; // Fallback for Google
        }

        String provider = request.getRequestURI().contains("github") ? "GITHUB" : "GOOGLE";

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            // Register new user
            user = new User();
            user.setEmail(email);
            user.setUsername(username);
            user.setFullName(name);
            user.setRole("DEVELOPER");
            user.setProvider(provider);
            user.setActive(true);
            user.setCreatedAt(LocalDateTime.now());
            authService.register(user);
        }

        // Redirect to frontend with success
        String redirectUrl = "http://localhost:8080/login/oauth2/code/" + provider.toLowerCase() + "?success=true";
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}