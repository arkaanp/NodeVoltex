package com.nodevoltex.backend.dto;

public class UserDTO {
    private String username;
    private String profilePictureUrl;
    private Double volforce;

    public UserDTO() {}

    public UserDTO(String username, String profilePictureUrl) {
        this.username = username;
        this.profilePictureUrl = profilePictureUrl;
        this.volforce = 0.0;
    }

    public UserDTO(String username, String profilePictureUrl, Double volforce) {
        this.username = username;
        this.profilePictureUrl = profilePictureUrl;
        this.volforce = volforce;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public Double getVolforce() {
        return volforce;
    }

    public void setVolforce(Double volforce) {
        this.volforce = volforce;
    }
}
