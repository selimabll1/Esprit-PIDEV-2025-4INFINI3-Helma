package com.helma.helmabackend.controller.user;

import com.helma.helmabackend.dto.user.UserProfileResponse;
import com.helma.helmabackend.dto.user.UserProfileUpsertRequest;
import com.helma.helmabackend.service.user.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    public UserProfileResponse getMyProfile() {
        return userProfileService.getMyProfile();
    }

    @PutMapping
    public UserProfileResponse updateMyProfile(@Valid @RequestBody UserProfileUpsertRequest request) {
        return userProfileService.upsertMyProfile(request);
    }
}