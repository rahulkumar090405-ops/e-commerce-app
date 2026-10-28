package com.spring.userauth.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spring.userauth.dto.ProfileUpdateRequest;
import com.spring.userauth.dto.UserProfileResponse;
import com.spring.userauth.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users/me")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserProfileResponse getMyProfile(Authentication authentication) {
        return userService.getProfileByEmail(authentication.getName());
    }

    @PatchMapping
    public UserProfileResponse updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody ProfileUpdateRequest request) {
        return userService.updateProfile(authentication.getName(), request);
    }
}
