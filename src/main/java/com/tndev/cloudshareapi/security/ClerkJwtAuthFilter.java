package com.tndev.cloudshareapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Collections;
@Component
@RequiredArgsConstructor
public class ClerkJwtAuthFilter extends OncePerRequestFilter {

    @Value("${clerk.issuer}")
    private String clerkIssuer;

    private final ClerkJwksProvider jwksProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        // 1. Skip webhooks and RETURN
        if (request.getRequestURI().contains("/webhooks")||request.getRequestURI().contains("/public")||request.getRequestURI().contains("/download")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extract Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN,
                    "Authorization header is missing or invalid");
            return;
        }

        try {
            String token = authHeader.substring(7);

            String[] chunks = token.split("\\.");
            if (chunks.length < 3) {
                sendError(response, HttpServletResponse.SC_FORBIDDEN,
                        "Invalid JWT Token Format");
                return;
            }

            String headerJson = new String(Base64.getUrlDecoder().decode(chunks[0]));
            ObjectMapper mapper = new ObjectMapper();
            JsonNode headerNode = mapper.readTree(headerJson);

            if (!headerNode.has("kid")) {
                sendError(response, HttpServletResponse.SC_FORBIDDEN,
                        "Token Header is missing the key ID");
                return;
            }

            String kid = headerNode.get("kid").asText();
            PublicKey publicKey = jwksProvider.getPublicKey(kid);

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .setAllowedClockSkewSeconds(60)
                    .requireIssuer(clerkIssuer)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String clerkId = claims.getSubject();

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            clerkId,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
                    );

            SecurityContextHolder.getContext().setAuthentication(authToken);

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN,
                    "Invalid JWT Token: " + e.getMessage());
        }
    }

    private void sendError(HttpServletResponse response, int code, String message) throws IOException {
        if (!response.isCommitted()) {
            response.sendError(code, message);
        }
    }
}
