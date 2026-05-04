package com.helma.helmabackend.service.user;

import com.helma.helmabackend.dto.user.UserProfileResponse;
import com.helma.helmabackend.dto.user.UserProfileUpsertRequest;
import com.helma.helmabackend.entity.user.User;
import com.helma.helmabackend.entity.user.UserProfile;
import com.helma.helmabackend.repository.user.UserProfileRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final CurrentUserService currentUserService;
    private final UserProfileRepository userProfileRepository;

    @Transactional
    public UserProfileResponse getMyProfile() {
        User currentUser = currentUserService.getCurrentUser();
        UserProfile profile = getOrCreateProfile(currentUser);
        return toResponse(currentUser, profile);
    }

    @Transactional
    public UserProfileResponse upsertMyProfile(UserProfileUpsertRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        UserProfile profile = getOrCreateProfile(currentUser);

        profile.setFirstName(request.firstName().trim());
        profile.setLastName(request.lastName().trim());
        profile.setPhoneNumber(trimToNull(request.phoneNumber()));
        profile.setDateOfBirth(request.dateOfBirth());
        profile.setGender(request.gender());
        profile.setCountry(trimToNull(request.country()));
        profile.setCity(trimToNull(request.city()));
        profile.setAddressLine(trimToNull(request.addressLine()));
        profile.setOccupation(trimToNull(request.occupation()));
        profile.setNationalId(trimToNull(request.nationalId()));
        profile.setBio(trimToNull(request.bio()));

        UserProfile saved = userProfileRepository.save(profile);
        return toResponse(currentUser, saved);
    }

    private UserProfile getOrCreateProfile(User user) {
        return userProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserProfile profile = new UserProfile();
                    profile.setUser(user);
                    profile.setFirstName("");
                    profile.setLastName("");
                    return userProfileRepository.save(profile);
                });
    }

    public UserProfileResponse toResponse(User user, UserProfile profile) {
        return new UserProfileResponse(
                profile.getId(),
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getPhoneNumber(),
                profile.getDateOfBirth(),
                profile.getGender(),
                profile.getCountry(),
                profile.getCity(),
                profile.getAddressLine(),
                profile.getOccupation(),
                profile.getNationalId(),
                profile.getBio(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}