package com.nodevoltex.game.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.nodevoltex.game.NodeVoltex;

import java.util.HashMap;
import java.util.Map;

public class TextureLoader {
    private static final Map<String, Texture> cache = new HashMap<>();
    private static final String BASE_URL = "http://localhost:8082";

    private static String transformCloudinaryUrl(String url) {
        if (url == null || url.isEmpty()) return url;
        String transformed = url;
        if (url.contains("cloudinary.com")) {
            // Case-insensitive replacement of .webp with .png
            transformed = transformed.replaceAll("(?i)\\.webp", ".png");
            // Swap auto format or webp format settings for png format
            transformed = transformed.replace("/f_auto/", "/f_png/");
            transformed = transformed.replace("/f_webp/", "/f_png/");
        }
        return transformed;
    }

    public interface TextureCallback {
        void onLoaded(Texture texture);
        void onError(Throwable t);
    }

    public static void loadTexture(String url, final TextureCallback callback) {
        url = transformCloudinaryUrl(url);
        if (url == null || url.isEmpty()) {
            callback.onError(new Exception("Empty URL"));
            return;
        }

        final String fullUrl;
        if (url.startsWith("http")) {
            fullUrl = url;
        } else {
            fullUrl = BASE_URL + (url.startsWith("/") ? "" : "/") + url;
        }

        if (cache.containsKey(fullUrl)) {
            callback.onLoaded(cache.get(fullUrl));
            return;
        }

        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
        request.setUrl(fullUrl);
        request.setTimeOut(10000);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                final byte[] bytes = httpResponse.getResult();
                Gdx.app.postRunnable(() -> {
                    try {
                        Pixmap pixmap = new Pixmap(bytes, 0, bytes.length);
                        Texture texture = new Texture(pixmap);
                        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                        pixmap.dispose();
                        cache.put(fullUrl, texture);
                        callback.onLoaded(texture);
                    } catch (Exception e) {
                        callback.onError(e);
                    }
                });
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.postRunnable(() -> callback.onError(t));
            }

            @Override
            public void cancelled() {
                Gdx.app.postRunnable(() -> callback.onError(new Exception("Cancelled")));
            }
        });
    }

    public static void loadIntoImage(String url, final Image image, final Texture defaultTexture) {
        if (url == null || url.isEmpty()) {
            if (defaultTexture != null) image.setDrawable(new TextureRegionDrawable(defaultTexture));
            return;
        }

        loadTexture(url, new TextureCallback() {
            @Override
            public void onLoaded(Texture texture) {
                image.setDrawable(new TextureRegionDrawable(texture));
            }

            @Override
            public void onError(Throwable t) {
                if (defaultTexture != null) image.setDrawable(new TextureRegionDrawable(defaultTexture));
            }
        });
    }
}
