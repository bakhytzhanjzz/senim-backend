package com.senim.backend.controller;

import com.senim.backend.domain.Agency;
import com.senim.backend.domain.User;
import com.senim.backend.dto.UserProfileResponse;
import com.senim.backend.repository.AgencyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final AgencyRepository agencyRepository;

    public UserController(AgencyRepository agencyRepository) {
        this.agencyRepository = agencyRepository;
    }

    /**
     * Returns the authenticated user's full profile along with their agency info.
     * Used by the Settings/Profile page.
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(
            @AuthenticationPrincipal User user) {

        UserProfileResponse.AgencyInfo agencyInfo = null;
        if (user.getAgencyId() != null) {
            Agency agency = agencyRepository.findById(user.getAgencyId()).orElse(null);
            if (agency != null) {
                agencyInfo = new UserProfileResponse.AgencyInfo(
                        agency.getId(),
                        agency.getName(),
                        agency.getSubscriptionTier(),
                        agency.getCreatedAt()
                );
            }
        }

        return ResponseEntity.ok(new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getCreatedAt(),
                agencyInfo
        ));
    }
}
