package com.nodevoltex.backend.controller;

import com.nodevoltex.backend.dto.UserDTO;
import com.nodevoltex.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getMe(@AuthenticationPrincipal UserDetails userDetails) {
        var user = userService.getUserByUsername(userDetails.getUsername());
        return ResponseEntity.ok(new UserDTO(user.getUsername(), user.getProfilePictureUrl(), user.getVolforce()));
    }

    @PostMapping("/profile-picture")
    public ResponseEntity<String> uploadProfilePicture(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file
    ) {
        userService.updateProfilePicture(userDetails.getUsername(), file);
        return ResponseEntity.ok("Profile picture updated successfully");
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<java.util.List<UserDTO>> getLeaderboard() {
        return ResponseEntity.ok(userService.getLeaderboard());
    }

    @GetMapping("/profile/{username}")
    public ResponseEntity<UserDTO> getProfile(@PathVariable String username) {
        var user = userService.getUserByUsername(username);
        return ResponseEntity.ok(new UserDTO(user.getUsername(), user.getProfilePictureUrl(), user.getVolforce()));
    }
}
