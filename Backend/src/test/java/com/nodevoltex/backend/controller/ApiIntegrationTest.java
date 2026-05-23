package com.nodevoltex.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nodevoltex.backend.dto.AuthRequest;
import com.nodevoltex.backend.dto.ScoreRequest;
import com.nodevoltex.backend.entity.User;
import com.nodevoltex.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.nodevoltex.backend.service.CloudinaryService cloudinaryService;

    private String jwtToken;
    private final String TEST_USER = "testUser_" + UUID.randomUUID().toString().substring(0, 8);
    private final String TEST_PASS = "password123";

    @BeforeEach
    void setUp() throws Exception {
        org.mockito.Mockito.when(cloudinaryService.uploadFile(org.mockito.Mockito.any()))
                .thenReturn("http://cloudinary.com/avatar.png");

        // 1. Register
        AuthRequest registerRequest = new AuthRequest(TEST_USER, TEST_PASS);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Extract token for subsequent requests
        String response = result.getResponse().getContentAsString();
        this.jwtToken = objectMapper.readTree(response).get("token").asText();
    }

    @Test
    void testAuthEndpoints() throws Exception {
        // Test Login
        AuthRequest loginRequest = new AuthRequest(TEST_USER, TEST_PASS);
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

        // Test Duplicate Register
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username already exists"));
    }

    @Test
    void testProfilePictureUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", MediaType.IMAGE_PNG_VALUE, "test image content".getBytes()
        );

        mockMvc.perform(multipart("/api/users/profile-picture")
                .file(file)
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(content().string("Profile picture updated successfully"));
    }

    @Test
    void testScoreSubmissionAndLeaderboard() throws Exception {
        // Refresh token to ensure security context
        AuthRequest loginRequest = new AuthRequest(TEST_USER, TEST_PASS);
        MvcResult authResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String currentToken = objectMapper.readTree(authResult.getResponse().getContentAsString()).get("token").asText();

        String mapId = "TestSong_EXH";
        ScoreRequest scoreRequest = new ScoreRequest();
        scoreRequest.setMapId(mapId);
        scoreRequest.setTitle("Test Song");
        scoreRequest.setArtist("Test Artist");
        scoreRequest.setDifficulty("EXH");
        scoreRequest.setLevel(10);
        scoreRequest.setScore(9000000);
        scoreRequest.setGrade("AA");
        scoreRequest.setMaxCombo(500);
        scoreRequest.setCriticals(450);
        scoreRequest.setNears(40);
        scoreRequest.setMisses(10);
        scoreRequest.setReplayDataJson("{}");

        // Submit Score
        mockMvc.perform(post("/api/scores/submit")
                .header("Authorization", "Bearer " + currentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(scoreRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playVolforce").value(0.888))
                .andExpect(jsonPath("$.newTotalVolforce").value(0.888))
                .andExpect(jsonPath("$.volforceGained").value(0.888));

        // Get Leaderboard
        mockMvc.perform(get("/api/scores/leaderboard/" + mapId)
                .header("Authorization", "Bearer " + currentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value(TEST_USER))
                .andExpect(jsonPath("$[0].score").value(9000000))
                .andExpect(jsonPath("$[0].grade").value("AA"));
                
        // Submit Higher Score (Update)
        scoreRequest.setScore(9500000);
        scoreRequest.setGrade("AAA");
        mockMvc.perform(post("/api/scores/submit")
                .header("Authorization", "Bearer " + currentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(scoreRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playVolforce").value(0.997))
                .andExpect(jsonPath("$.newTotalVolforce").value(0.997))
                .andExpect(jsonPath("$.volforceGained").value(0.109));

        // Verify User Profile Volforce
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + currentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(TEST_USER))
                .andExpect(jsonPath("$.volforce").value(0.997));

        // Verify Update
        mockMvc.perform(get("/api/scores/leaderboard/" + mapId)
                .header("Authorization", "Bearer " + currentToken))
                .andExpect(jsonPath("$[0].score").value(9500000))
                .andExpect(jsonPath("$[0].grade").value("AAA"));
    }

    @Test
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(post("/api/scores/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
