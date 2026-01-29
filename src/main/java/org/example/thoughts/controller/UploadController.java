package org.example.thoughts.controller;

import org.example.thoughts.service.VkUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/// Создаём контроллер на POST
@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
public class UploadController {
    private final VkUploadService vkUploadService;

    public UploadController(VkUploadService vkUploadService) {
        this.vkUploadService = vkUploadService;
    }
    /// Получение фотографии из телеграма
    @PostMapping("/photo")
    public ResponseEntity<Map<String, Object>> uploadPhoto(@RequestParam("photo") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }

            if (!file.getContentType().startsWith("image/")) {
                response.put("success", false);
                response.put("message", "File must be an image");
                return ResponseEntity.badRequest().body(response);
            }

            String photoUrl = vkUploadService.uploadPhotoToVk(
                    file.getBytes(),
                    file.getOriginalFilename()
            );

            response.put("success", true);
            response.put("message", "Photo uploaded successfully");
            response.put("photoUrl", photoUrl);
            response.put("photoId", "uploaded_" + System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Upload failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    /// Перевод фотографии из *.png формата в байты и загрузка
    @PostMapping("/photo-bytes")
    public ResponseEntity<Map<String, Object>> uploadPhotoBytes(@RequestBody byte[] photoData,
                                                                @RequestParam String filename) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (photoData == null || photoData.length == 0) {
                response.put("success", false);
                response.put("message", "Photo data is empty");
                return ResponseEntity.badRequest().body(response);
            }

            String photoUrl = vkUploadService.uploadPhotoToVk(photoData, filename);

            response.put("success", true);
            response.put("message", "Photo uploaded successfully");
            response.put("photoUrl", photoUrl);
            response.put("photoId", "uploaded_" + System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Upload failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /// Эндпоинт контроля состояния
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> uploadHealthCheck() {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean vkAccess = vkUploadService.checkVkAccess();
            response.put("status", "OK");
            response.put("vk_api_accessible", vkAccess);
            response.put("service", "VK Upload Service");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}