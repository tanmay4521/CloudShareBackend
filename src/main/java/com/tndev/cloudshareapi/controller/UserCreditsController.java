package com.tndev.cloudshareapi.controller;

import com.tndev.cloudshareapi.document.UserCredits;
import com.tndev.cloudshareapi.dto.UserCreditsDTO;
import com.tndev.cloudshareapi.service.UserCreditsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserCreditsController {
    private final UserCreditsService userCreditsService;

    @GetMapping("/credits")
    public ResponseEntity<?> getUserCredits() {
        UserCredits userCredits=userCreditsService.getUserCredits();
        UserCreditsDTO response=UserCreditsDTO.builder()
                .credits(userCredits.getCredits())
                .plan(userCredits.getPlan())
                .build();
        return ResponseEntity.ok(response);
    }
}
