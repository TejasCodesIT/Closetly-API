package com.closetly.closetly_backend.security;

import com.closetly.closetly_backend.user.entity.Role;
import com.closetly.closetly_backend.user.entity.User;
import com.closetly.closetly_backend.user.repository.RoleRepository;
import com.closetly.closetly_backend.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.oauth2.redirectUri:http://localhost:3000/oauth2/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid OAuth2 authentication");
            return;
        }

        OAuth2User oauth2User = oauthToken.getPrincipal();
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String picture = oauth2User.getAttribute("picture");

        if (email == null || email.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Google account email not available");
            return;
        }

        User user = Objects.requireNonNull(userRepository.findByEmail(email).orElseGet(() -> {
            Role userRole = roleRepository.findByName(Role.RoleName.USER)
                    .orElseThrow(() -> new IllegalArgumentException("Default role not found"));
            User created = User.builder()
                    .email(email)
                    .password("") // not used for oauth users
                    .fullName(name != null ? name : email)
                    .profileImageUrl(picture)
                    .enabled(true)
                    .emailVerified(true)
                    .roles(Set.of(userRole))
                    .build();
            return userRepository.save(created);
        }));

        // fill missing data
        boolean changed = false;
        if ((user.getFullName() == null || user.getFullName().isBlank()) && name != null) {
            user.setFullName(name);
            changed = true;
        }
        if ((user.getProfileImageUrl() == null || user.getProfileImageUrl().isBlank()) && picture != null) {
            user.setProfileImageUrl(picture);
            changed = true;
        }
        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            user.setEnabled(true);
            user.setVerificationToken(null);
            changed = true;
        }
        if (changed) {
            user = Objects.requireNonNull(userRepository.save(user));
        }

        // Build an Authentication compatible with JwtTokenProvider (expects CustomUserDetails principal)
        CustomUserDetails principal = new CustomUserDetails(user);
        var jwtAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        String jwt = jwtTokenProvider.generateToken(jwtAuth);

        String location = redirectUri + (redirectUri.contains("?") ? "&" : "?") + "token=" + jwt;
        response.sendRedirect(location);
    }
}

