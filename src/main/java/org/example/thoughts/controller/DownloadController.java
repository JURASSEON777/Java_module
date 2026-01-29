package org.example.thoughts.controller;

import org.example.thoughts.service.VkDownloadService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DownloadController {

    private final VkDownloadService vkDownloadService;

    public DownloadController(VkDownloadService vkDownloadService) {
        this.vkDownloadService = vkDownloadService;
    }

    @GetMapping("/random-photo")
    public ResponseEntity<byte[]> getRandomPhoto() {
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
                    .body(("Error: " + e.getMessage()).getBytes());
        }
    }
    
    @GetMapping("/filtered/photo-by-year")
    public ResponseEntity<byte[]> getPhotoByYear(@RequestParam("year") int year) {
        try {
            System.out.println("Request for photo by year: " + year);

            // Проверка валидности года (например, от 2000 до 2030)
            if (year < 2000 || year > 2030) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(("Invalid year. Please provide year between 2000 and 2030.").getBytes());
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
                    .body(("Error: " + e.getMessage()).getBytes());
        } catch (Exception e) {
            System.err.println("Unexpected error in getPhotoByYear: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Unexpected error: " + e.getMessage()).getBytes());
        }
    }
    // Новый эндпоинт для проверки доступа к альбому
    @GetMapping("/check-access")
    public ResponseEntity<Map<String, String>> checkAccess() {
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

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "VK Photo Service");
        return ResponseEntity.ok(response);
    }
}