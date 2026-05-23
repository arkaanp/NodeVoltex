package com.nodevoltex.game.networking;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpParametersUtils;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.nodevoltex.game.managers.SettingsManager;

import java.util.HashMap;
import java.util.Map;

public class NetworkManager {
    private static final String BASE_URL = "https://nodevoltex.onrender.com";
    private static final Json json = new Json();
    static {
        json.setOutputType(JsonWriter.OutputType.json);
        json.setIgnoreUnknownFields(true);
        json.setTypeName(null); // Disable @class being added to JSON
    }

    public interface NetworkCallback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    // Static request DTOs to ensure LibGDX Json doesn't add class metadata
    public static class AuthRequest {
        public String username;
        public String password;
    }

    public static class ScoreRequest {
        public String mapId;
        public String title;
        public String artist;
        public String difficulty;
        public Integer level;
        public Integer score;
        public String grade;
        public Integer maxCombo;
        public Integer sCriticals;
        public Integer criticals;
        public Integer nears;
        public Integer mids;
        public Integer fars;
        public Integer misses;
        public Integer laserTicks;
        public Integer laserMisses;
        public Integer early;
        public Integer late;
        public String replayDataJson;
    }

    public static void register(String username, String password, final NetworkCallback<String> callback) {
        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.POST);
        request.setUrl(BASE_URL + "/auth/register");
        request.setHeader("Content-Type", "application/json");

        AuthRequest data = new AuthRequest();
        data.username = username;
        data.password = password;
        request.setContent(json.toJson(data));

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(final Net.HttpResponse httpResponse) {
                final String responseText = httpResponse.getResultAsString();
                final int statusCode = httpResponse.getStatus().getStatusCode();
                Gdx.app.postRunnable(() -> {
                    if (statusCode == 200 || statusCode == 201) {
                        callback.onSuccess("Registration successful");
                    } else {
                        callback.onError("Registration failed: " + statusCode + " - " + responseText);
                    }
                });
            }

            @Override
            public void failed(final Throwable t) {
                Gdx.app.postRunnable(() -> callback.onError("Network error: " + t.getMessage()));
            }

            @Override
            public void cancelled() {
                Gdx.app.postRunnable(() -> callback.onError("Request cancelled"));
            }
        });
    }

    public static void login(String username, String password, final NetworkCallback<String> callback) {
        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.POST);
        request.setUrl(BASE_URL + "/auth/login");
        request.setHeader("Content-Type", "application/json");

        AuthRequest data = new AuthRequest();
        data.username = username;
        data.password = password;
        request.setContent(json.toJson(data));

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(final Net.HttpResponse httpResponse) {
                final String responseText = httpResponse.getResultAsString();
                final int statusCode = httpResponse.getStatus().getStatusCode();

                Gdx.app.postRunnable(() -> {
                    if (statusCode == 200) {
                        try {
                            com.badlogic.gdx.utils.JsonValue root = new com.badlogic.gdx.utils.JsonReader().parse(responseText);
                            String token = root.getString("token");
                            SettingsManager.setAuthToken(token);
                            SettingsManager.setUserName(username);

                            // Fetch profile after login to get PFP
                            fetchUserProfile(new NetworkCallback<Void>() {
                                @Override public void onSuccess(Void result) { callback.onSuccess(token); }
                                @Override public void onError(String message) { callback.onSuccess(token); } // Continue anyway
                            });
                        } catch (Exception e) {
                            callback.onError("Failed to parse login response: " + e.getMessage());
                        }
                    } else {
                        callback.onError("Login failed: " + statusCode + " - " + responseText);
                    }
                });
            }

            @Override
            public void failed(final Throwable t) {
                Gdx.app.postRunnable(() -> callback.onError("Network error: " + t.getMessage()));
            }

            @Override
            public void cancelled() {
                Gdx.app.postRunnable(() -> callback.onError("Request cancelled"));
            }
        });
    }

    public static void fetchUserProfile(final NetworkCallback<Void> callback) {
        String token = SettingsManager.getAuthToken();
        if (token.isEmpty()) {
            callback.onError("Not logged in");
            return;
        }

        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
        request.setUrl(BASE_URL + "/users/me");
        request.setHeader("Authorization", "Bearer " + token);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                final String responseText = httpResponse.getResultAsString();
                final int statusCode = httpResponse.getStatus().getStatusCode();

                Gdx.app.postRunnable(() -> {
                    if (statusCode == 200) {
                        try {
                            com.badlogic.gdx.utils.JsonValue root = new com.badlogic.gdx.utils.JsonReader().parse(responseText);
                            if (root.has("profilePictureUrl") && !root.get("profilePictureUrl").isNull()) {
                                SettingsManager.setProfilePictureUrl(root.getString("profilePictureUrl"));
                            } else {
                                SettingsManager.setProfilePictureUrl("");
                            }
                            callback.onSuccess(null);
                        } catch (Exception e) {
                            callback.onError("Failed to parse user profile: " + e.getMessage());
                        }
                    } else {
                        callback.onError("Failed to fetch profile: " + statusCode);
                    }
                });
            }

            @Override public void failed(Throwable t) { Gdx.app.postRunnable(() -> callback.onError(t.getMessage())); }
            @Override public void cancelled() { Gdx.app.postRunnable(() -> callback.onError("Cancelled")); }
        });
    }

    public static void uploadProfilePicture(com.badlogic.gdx.files.FileHandle file, final NetworkCallback<String> callback) {
        String token = SettingsManager.getAuthToken();
        if (token.isEmpty()) {
            callback.onError("Not logged in");
            return;
        }

        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.POST);
        request.setUrl(BASE_URL + "/users/profile-picture");
        request.setHeader("Authorization", "Bearer " + token);

        // LibGDX Net doesn't natively support easy multipart/form-data with binary files.
        // We'll have to manually construct the body or use a different approach.
        // For simplicity, let's try to use the raw bytes if the backend supports it,
        // but the backend is expecting MultipartFile.

        // Manual multipart construction:
        String boundary = "---------------------------" + System.currentTimeMillis();
        request.setHeader("Content-Type", "multipart/form-data; boundary=" + boundary);

        byte[] fileBytes = file.readBytes();
        String entryHeader = "--" + boundary + "\r\n" +
                             "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.name() + "\"\r\n" +
                             "Content-Type: image/png\r\n\r\n";
        String footer = "\r\n--" + boundary + "--\r\n";

        byte[] headerBytes = entryHeader.getBytes();
        byte[] footerBytes = footer.getBytes();

        byte[] completeBody = new byte[headerBytes.length + fileBytes.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, completeBody, 0, headerBytes.length);
        System.arraycopy(fileBytes, 0, completeBody, headerBytes.length, fileBytes.length);
        System.arraycopy(footerBytes, 0, completeBody, headerBytes.length + fileBytes.length, footerBytes.length);

        request.setContent(new java.io.ByteArrayInputStream(completeBody), completeBody.length);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                final int statusCode = httpResponse.getStatus().getStatusCode();
                final String responseText = httpResponse.getResultAsString();
                Gdx.app.postRunnable(() -> {
                    if (statusCode == 200) {
                        // Refresh profile to get the new URL
                        fetchUserProfile(new NetworkCallback<Void>() {
                            @Override public void onSuccess(Void result) { callback.onSuccess("Upload successful"); }
                            @Override public void onError(String message) { callback.onSuccess("Upload successful (refetch failed)"); }
                        });
                    } else {
                        callback.onError("Upload failed: " + statusCode + " " + responseText);
                    }
                });
            }

            @Override public void failed(Throwable t) { Gdx.app.postRunnable(() -> callback.onError(t.getMessage())); }
            @Override public void cancelled() { Gdx.app.postRunnable(() -> callback.onError("Cancelled")); }
        });
    }

    public static void submitScore(ScoreRequest scoreData, final NetworkCallback<String> callback) {
        String token = SettingsManager.getAuthToken();
        if (token.isEmpty()) {
            callback.onError("Not logged in");
            return;
        }

        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.POST);
        request.setUrl(BASE_URL + "/scores/submit");
        request.setHeader("Content-Type", "application/json");
        request.setHeader("Authorization", "Bearer " + token);
        request.setContent(json.toJson(scoreData));

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(final Net.HttpResponse httpResponse) {
                final String responseText = httpResponse.getResultAsString();
                final int statusCode = httpResponse.getStatus().getStatusCode();

                Gdx.app.postRunnable(() -> {
                    if (statusCode == 200 || statusCode == 201) {
                        callback.onSuccess("Score submitted");
                    } else {
                        callback.onError("Submission failed: " + statusCode + " - " + responseText);
                    }
                });
            }

            @Override
            public void failed(final Throwable t) {
                Gdx.app.postRunnable(() -> callback.onError("Network error: " + t.getMessage()));
            }

            @Override
            public void cancelled() {
                Gdx.app.postRunnable(() -> callback.onError("Request cancelled"));
            }
        });
    }

    public static void getLeaderboard(String mapId, final NetworkCallback<String> callback) {
        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
        String token = com.nodevoltex.game.managers.SettingsManager.getAuthToken();
        if (!token.isEmpty()) {
            request.setHeader("Authorization", "Bearer " + token);
        }

        // Ensure the ID is URL-encoded for songs with spaces (e.g., "Heat Abnormal")
        String encodedId = mapId.replace(" ", "%20");
        request.setUrl(BASE_URL + "/scores/leaderboard/" + encodedId);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(final Net.HttpResponse httpResponse) {
                final String responseText = httpResponse.getResultAsString();
                final int statusCode = httpResponse.getStatus().getStatusCode();

                Gdx.app.postRunnable(() -> {
                    if (statusCode == 200) {
                        callback.onSuccess(responseText);
                    } else {
                        callback.onError("Failed to fetch leaderboard: " + statusCode + " - " + responseText);
                    }
                });
            }

            @Override
            public void failed(final Throwable t) {
                Gdx.app.postRunnable(() -> callback.onError("Network error: " + t.getMessage()));
            }

            @Override
            public void cancelled() {
                Gdx.app.postRunnable(() -> callback.onError("Request cancelled"));
            }
        });
    }
}
