package com.tndev.cloudshareapi.controller;

import com.tndev.cloudshareapi.dto.ProfileDTO;
import com.tndev.cloudshareapi.service.ProfileService;
import com.tndev.cloudshareapi.service.UserCreditsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class ClerkWebhookController {

    @Value("${clerk.webhook.secret}")
    private String webhookSecret;

    private final ProfileService profileService;
    private final UserCreditsService userCreditsService;

    @PostMapping("/clerk")
    public ResponseEntity<?> handleClerkWebhook(
            @RequestHeader("svix-id") String svixId,
            @RequestHeader("svix-timestamp") String svixTimestamp,
            @RequestHeader("svix-signature") String svixSignature,
            @RequestBody String payload
    ) {
        try {
            boolean isValid = verifyWebhookSignature(svixId, svixTimestamp, svixSignature, payload);
            if (!isValid) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid webhook signature");
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(payload);

            String eventType = rootNode.path("type").asText();
            JsonNode data = rootNode.path("data");

            switch (eventType) {
                case "user.created":
                    handleUserCreated(data);
                    break;

                case "user.updated":
                    handleUserUpdated(data);
                    break;

                case "user.deleted":
                    handleUserDeleted(data);
                    break;
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    private void handleUserDeleted(JsonNode data) {
        String clerkId = data.path("id").asText();
        profileService.deleteProfile(clerkId);
    }

    private void handleUserUpdated(JsonNode data) {
        String email = extractEmail(data);
        String clerkId = data.path("id").asText();
        String firstName = data.path("first_name").asText("");
        String lastName = data.path("last_name").asText("");
        String photoUrl = data.path("image_url").asText("");

        ProfileDTO updatedProfile = ProfileDTO.builder()
                .clerkId(clerkId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .photoUrl(photoUrl)
                .build();

        updatedProfile = profileService.updateProfile(updatedProfile);

        if (updatedProfile == null) {
            handleUserCreated(data);
        }
    }

    private void handleUserCreated(JsonNode data) {
        String email = extractEmail(data);
        String clerkId = data.path("id").asText();
        String firstName = data.path("first_name").asText("");
        String lastName = data.path("last_name").asText("");
        String photoUrl = data.path("image_url").asText("");

        ProfileDTO newProfile = ProfileDTO.builder()
                .clerkId(clerkId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .photoUrl(photoUrl)
                .build();

        profileService.createProfile(newProfile);
        userCreditsService.getUserCredits(clerkId);
    }

    private String extractEmail(JsonNode data) {
        JsonNode emailArray = data.path("email_addresses");

        if (emailArray.isArray() && emailArray.size() > 0) {
            return emailArray.get(0).path("email_address").asText("");
        }

        String primaryId = data.path("primary_email_address_id").asText("");
        if (!primaryId.isEmpty() && emailArray.isArray()) {
            for (JsonNode obj : emailArray) {
                if (obj.path("id").asText("").equals(primaryId)) {
                    return obj.path("email_address").asText("");
                }
            }
        }
        return "";
    }

    private boolean verifyWebhookSignature(String svixId, String svixTimestamp,
                                           String svixSignature, String payload) {
        return true;
    }
}
