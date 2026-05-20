package com.nodevoltex.backend.service;

import com.nodevoltex.backend.dto.AuthRequest;
import com.nodevoltex.backend.dto.AuthResponse;
import com.nodevoltex.backend.entity.User;
import com.nodevoltex.backend.repository.UserRepository;
import com.nodevoltex.backend.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final FileStorageService fileStorageService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager, FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.fileStorageService = fileStorageService;
    }

    public AuthResponse register(AuthRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        var user = new User(null, request.getUsername(), passwordEncoder.encode(request.getPassword()), null);
        userRepository.save(user);
        
        var userDetails = org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities("USER")
                .build();
        
        var jwtToken = jwtService.generateToken(userDetails);
        return new AuthResponse(jwtToken);
    }

    public AuthResponse authenticate(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow();
        
        var userDetails = org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities("USER")
                .build();
                
        var jwtToken = jwtService.generateToken(userDetails);
        return new AuthResponse(jwtToken);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void updateProfilePicture(String username, org.springframework.web.multipart.MultipartFile file) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        String fileName = fileStorageService.storeFile(file);
        String fileUrl = "/uploads/" + fileName;
        
        user.setProfilePictureUrl(fileUrl);
        userRepository.save(user);
    }
}
