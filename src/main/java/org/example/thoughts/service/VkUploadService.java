package org.example.thoughts.service;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class VkUploadService {

    @Value("${vk.group.id}")
    private String groupId;

    @Value("${vk.album.id}")
    private String albumId;

    @Value("${vk.access.token}")
    private String accessToken;

    private final RestTemplate restTemplate;
    private final CloseableHttpClient httpClient;

    public VkUploadService(RestTemplate restTemplate, CloseableHttpClient httpClient) {
        this.restTemplate = restTemplate;
        this.httpClient = httpClient;
    }

    public String uploadPhotoToVk(byte[] photoData, String filename) {
        try {
            System.out.println("Starting VK upload process...");

            // 1. Получаем URL для загрузки
            String uploadServerUrl = getUploadServer();
            System.out.println("Got upload server: " + uploadServerUrl);

            // 2. Загружаем фото на сервер VK
            JSONObject uploadResult = uploadPhotoToServer(uploadServerUrl, photoData, filename);
            System.out.println("Upload result: " + uploadResult);

            // 3. Сохраняем фото в альбом
            String photoId = savePhotoToAlbum(uploadResult);
            System.out.println("Photo saved with ID: " + photoId);

            // 4. Получаем URL загруженного фото
            String photoUrl = getPhotoUrl(photoId);
            System.out.println("Photo URL: " + photoUrl);

            return photoUrl;

        } catch (Exception e) {
            System.err.println("Error uploading photo to VK: " + e.getMessage());
            throw new RuntimeException("Failed to upload photo to VK: " + e.getMessage(), e);
        }
    }

    private String getUploadServer() {
        try {
            String url = "https://api.vk.com/method/photos.getUploadServer?" +
                    "group_id=" + groupId +
                    "&album_id=" + albumId +
                    "&access_token=" + accessToken +
                    "&v=5.131";

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JSONObject jsonResponse = new JSONObject(response.getBody());

            if (jsonResponse.has("error")) {
                throw new RuntimeException("VK API Error: " + jsonResponse.getJSONObject("error").getString("error_msg"));
            }

            return jsonResponse.getJSONObject("response").getString("upload_url");

        } catch (Exception e) {
            throw new RuntimeException("Failed to get upload server: " + e.getMessage(), e);
        }
    }

    private JSONObject uploadPhotoToServer(String uploadUrl, byte[] photoData, String filename) {
        try {
            HttpPost uploadFile = new HttpPost(uploadUrl);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody(
                    "file1",
                    photoData,
                    ContentType.APPLICATION_OCTET_STREAM,
                    filename
            );

            HttpEntity multipart = builder.build();
            uploadFile.setEntity(multipart);

            try (CloseableHttpResponse response = httpClient.execute(uploadFile)) {
                String responseString = EntityUtils.toString(response.getEntity());
                System.out.println("Upload server response: " + responseString);
                return new JSONObject(responseString);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload photo to VK server: " + e.getMessage(), e);
        }
    }

    private String savePhotoToAlbum(JSONObject uploadResult) {
        try {
            String url = "https://api.vk.com/method/photos.save?" +
                    "group_id=" + groupId +
                    "&album_id=" + albumId +
                    "&server=" + uploadResult.getInt("server") +
                    "&photos_list=" + uploadResult.getString("photos_list") +
                    "&hash=" + uploadResult.getString("hash") +
                    "&access_token=" + accessToken +
                    "&v=5.131";

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JSONObject jsonResponse = new JSONObject(response.getBody());

            if (jsonResponse.has("error")) {
                throw new RuntimeException("VK API Error: " + jsonResponse.getJSONObject("error").getString("error_msg"));
            }

            JSONArray photos = jsonResponse.getJSONArray("response");
            return photos.getJSONObject(0).getString("id");

        } catch (Exception e) {
            throw new RuntimeException("Failed to save photo to album: " + e.getMessage(), e);
        }
    }

    private String getPhotoUrl(String photoId) {
        try {
            String url = "https://api.vk.com/method/photos.getById?" +
                    "photos=-" + groupId + "_" + photoId +
                    "&access_token=" + accessToken +
                    "&v=5.131";

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JSONObject jsonResponse = new JSONObject(response.getBody());

            if (jsonResponse.has("error")) {
                throw new RuntimeException("VK API Error: " + jsonResponse.getJSONObject("error").getString("error_msg"));
            }

            JSONArray photos = jsonResponse.getJSONArray("response");
            JSONObject photo = photos.getJSONObject(0);
            JSONArray sizes = photo.getJSONArray("sizes");

            return sizes.getJSONObject(sizes.length() - 1).getString("url");

        } catch (Exception e) {
            throw new RuntimeException("Failed to get photo URL: " + e.getMessage(), e);
        }
    }

    public boolean checkVkAccess() {
        try {
            String url = "https://api.vk.com/method/photos.getAlbums?" +
                    "owner_id=-" + groupId +
                    "&access_token=" + accessToken +
                    "&v=5.131";

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JSONObject jsonResponse = new JSONObject(response.getBody());

            return !jsonResponse.has("error");
        } catch (Exception e) {
            return false;
        }
    }
}
