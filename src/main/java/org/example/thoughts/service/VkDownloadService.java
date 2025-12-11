package org.example.thoughts.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Random;

@Service
public class VkDownloadService {

    @Value("${vk.group.id}")
    private String groupId;

    @Value("${vk.album.id}")
    private String albumId;

    @Value("${vk.access.token}")
    private String accessToken;

    private final RestTemplate restTemplate;
    private final Random random = new Random();

    public VkDownloadService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public byte[] getRandomPhotoFromAlbum() {
        try {
            System.out.println("Getting photos from album: " + albumId + " in group: " + groupId);

            // Формируем URL с токеном пользователя
            String vkApiUrl = "https://api.vk.com/method/photos.get?" +
                    "owner_id=-" + groupId +
                    "&album_id=" + albumId +
                    "&access_token=" + accessToken +
                    "&v=5.131" +
                    "&count=1000";

            // Добавляем заголовки
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Отправляем запрос к VK API
            ResponseEntity<String> response = restTemplate.exchange(
                    vkApiUrl, HttpMethod.GET, entity, String.class);

            JSONObject jsonResponse = new JSONObject(response.getBody());


            if (jsonResponse.has("error")) {
                JSONObject error = jsonResponse.getJSONObject("error");
                String errorMsg = error.getString("error_msg");
                int errorCode = error.getInt("error_code");

                throw new RuntimeException("VK API Error (" + errorCode + "): " + errorMsg);
            }

            JSONArray photos = jsonResponse.getJSONObject("response").getJSONArray("items");

            if (photos.length() == 0) {
                throw new RuntimeException("No photos found in album: " + albumId);
            }

            System.out.println("Found " + photos.length() + " photos in album");

            // Выбираем случайное фото
            JSONObject randomPhoto = photos.getJSONObject(random.nextInt(photos.length()));

            // Получаем URL в максимальном качестве
            JSONArray sizes = randomPhoto.getJSONArray("sizes");
            String photoUrl = sizes.getJSONObject(sizes.length() - 1).getString("url");

            System.out.println("Downloading photo from: " + photoUrl);

            // Скачиваем фото
            ResponseEntity<byte[]> photoResponse = restTemplate.exchange(
                    photoUrl, HttpMethod.GET, entity, byte[].class);

            if (photoResponse.getStatusCode().is2xxSuccessful() && photoResponse.getBody() != null) {
                System.out.println("Successfully downloaded photo, size: " + photoResponse.getBody().length + " bytes");
                return photoResponse.getBody();
            } else {
                throw new RuntimeException("Failed to download photo, status: " + photoResponse.getStatusCode());
            }

        } catch (Exception e) {
            System.err.println("Error in VkPhotoService: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to get photo from VK album: " + e.getMessage(), e);
        }
    }

    // Метод для проверки доступности альбома
    public String checkAlbumAccess() {
        try {
            String url = "https://api.vk.com/method/photos.getAlbums?" +
                    "owner_id=-" + groupId +
                    "&album_ids=" + albumId +
                    "&access_token=" + accessToken +
                    "&v=5.131";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JSONObject jsonResponse = new JSONObject(response.getBody());

            if (jsonResponse.has("error")) {
                return "Error: " + jsonResponse.getJSONObject("error").getString("error_msg");
            }

            JSONArray albums = jsonResponse.getJSONObject("response").getJSONArray("items");
            if (albums.length() > 0) {
                JSONObject album = albums.getJSONObject(0);
                return "Album '" + album.getString("title") + "' accessible. Photos: " + album.getInt("size");
            }

            return "Album not found";

        } catch (Exception e) {
            return "Error checking album: " + e.getMessage();
        }
    }
}