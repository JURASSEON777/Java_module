package org.example.thoughts.controller;

import org.example.thoughts.service.VkDownloadService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;

/// Создаём контроллер на GET
@RestController
@RequestMapping("/api")
public class DownloadController {

    private final VkDownloadService vkDownloadService;

    @Value("${service.api-key}")
    private String apiKey;

    public DownloadController(VkDownloadService vkDownloadService) {
        this.vkDownloadService = vkDownloadService;
    }

    /// Получение HTTP на работу с получением рандомной фотографией
    @GetMapping("/random-photo")
    public ResponseEntity<byte[]> getRandomPhoto(
            @RequestHeader(value = "X-API-Key", required = false) String suppliedKey) {
        if (!apiKey.equals(suppliedKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            byte[] photoData = vkDownloadService.getRandomPhotoFromAlbum();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setContentLength(photoData.length);
            headers.setContentDispositionFormData("attachment", "random_photo.jpg");

            return new ResponseEntity<>(photoData, headers, HttpStatus.OK);

        } catch (Exception e) {
            System.err.println("Error in controller: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    /// Получение HTTP на работу с получением рандомной фотографией, отсортированной по годам
    @GetMapping("/filtered/photo-by-year")
    public ResponseEntity<byte[]> getPhotoByYear(
            @RequestHeader(value = "X-API-Key", required = false) String suppliedKey,
            @RequestParam("year") int year) {
        if (!apiKey.equals(suppliedKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            System.out.println("Request for photo by year: " + year);

            /// Проверка валидности года
            if (year < 2000 || year > 2030) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(("Invalid year").getBytes());
            }

            byte[] photoData = vkDownloadService.getRandomPhotoByYear(year);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setContentLength(photoData.length);
            headers.setContentDispositionFormData("attachment", "photo_" + year + ".jpg");

            return new ResponseEntity<>(photoData, headers, HttpStatus.OK);

        } catch (RuntimeException e) {
            System.err.println("Error getting photo for year " + year + ": " + e.getMessage());

            // Если фото не найдено, возвращаем 404 с сообщением
            if (e.getMessage() != null && e.getMessage().contains("No photos found for year")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(("No photos found for year: " + year).getBytes());
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        } catch (Exception e) {
            System.err.println("Unexpected error in getPhotoByYear: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }
    /// Эндпоинт для проверки доступа к альбому
    @GetMapping("/check-access")
    public ResponseEntity<Map<String, String>> checkAccess(
            @RequestHeader(value = "X-API-Key", required = false) String suppliedKey) {
        if (!apiKey.equals(suppliedKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            String accessInfo = vkDownloadService.checkAlbumAccess();
            Map<String, String> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", accessInfo);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "ERROR");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /// Эндпоинт для проверки текущего состояния сервиса
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "VK Photo Service");
        return ResponseEntity.ok(response);
    }
}
