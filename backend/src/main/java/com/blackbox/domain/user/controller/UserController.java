package com.blackbox.domain.user.controller;

import com.blackbox.domain.user.dto.UserDto;
import com.blackbox.domain.user.entity.User;
import com.blackbox.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDto.ProfileResponse> getMyProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getProfile(user.getId()));
    }

    @PutMapping("/me/consent")
    public ResponseEntity<Void> updateConsent(
            @AuthenticationPrincipal User user,
            @RequestBody UserDto.ConsentRequest req) {
        userService.updateConsent(user.getId(), req);
        return ResponseEntity.noContent().build();
    }
}
