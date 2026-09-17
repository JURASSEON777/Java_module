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

/// Задаём значения сервиса
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

    /// Начинаем процесс подключения к ВК
    public String uploadPhotoToVk(byte[] photoData, String filename) {
        try {
            System.out.println("Starting VK upload process...");

            if (photoData == null || photoData.length < 8 ||
                    photoData[0] != (byte) 0x89 || photoData[1] != 'P' ||
                    photoData[2] != 'N' || photoData[3] != 'G' ||
                    photoData[4] != 0x0D || photoData[5] != 0x0A ||
                    photoData[6] != 0x1A || photoData[7] != 0x0A) {
                System.err.println("[ERROR] Uploaded data does NOT have a valid PNG signature.");
                throw new RuntimeException("Uploaded data is not a valid PNG image.");
            } else {
                System.out.println("[DEBUG] PNG signature check PASSED. Data length: " + photoData.length + " bytes.");
            }

            /// Получаем URL для загрузки
            String uploadServerUrl = getUploadServer();

            /// Загружаем фото на сервер VK
            JSONObject uploadResult = uploadPhotoToServer(uploadServerUrl, photoData, filename);

            /// Сохраняем фото в альбом
            String photoId = savePhotoToAlbum(uploadResult);

            /// Получаем URL загруженного фото и сразу возвращаем его
            return getPhotoUrl(photoId);

        } catch (Exception e) {
            System.err.println("Error uploading photo to VK: " + e.getMessage());
            throw new RuntimeException("Failed to upload photo to VK: " + e.getMessage(), e);
        }
    }

    /// Подключение к серверам ВК
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

    /// Загрузка фотографии на сервер
    private JSONObject uploadPhotoToServer(String uploadUrl, byte[] photoData, String filename) {
        try {
            HttpPost uploadFile = new HttpPost(uploadUrl);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody(
                    "file1",
                    photoData,
                    ContentType.create("image/png"),
                    filename == null ? "upload.png" : filename.replaceAll("[^a-zA-Z0-9._-]", "_")
            );

            HttpEntity multipart = builder.build();
            uploadFile.setEntity(multipart);

            try (CloseableHttpResponse response = httpClient.execute(uploadFile)) {
                String responseString = EntityUtils.toString(response.getEntity());
                int status = response.getStatusLine().getStatusCode();
                if (status < 200 || status >= 300) {
                    throw new RuntimeException("VK upload server returned HTTP " + status);
                }
                return new JSONObject(responseString);
            }

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to upload photo to VK server: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to upload photo to VK server: " + e.getMessage(), e);
        }
    }

    /// Сохранение фотографии в КОНКРЕТНОМ альбоме в ВК
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
            Object idObj = photos.getJSONObject(0).get("id");
            String photoId = String.valueOf(idObj);
            System.out.println("[DEBUG] Photo saved with ID (as String): " + photoId);
            return photoId;

        } catch (Exception e) {
            throw new RuntimeException("Failed to save photo to album: " + e.getMessage(), e);
        }
    }

    /// Проверка URL фотографии
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


    /// Проверка доступа к VK
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
