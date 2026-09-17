package org.example.thoughts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.config.RequestConfig;
import java.time.Duration;

@SpringBootApplication
public class ThoughtsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThoughtsServiceApplication.class, args);
    }

    /// Бины для запуска сервисов
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(45))
                .build();
    }

    @Bean
    public CloseableHttpClient httpClient() {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(10_000)
                .setConnectionRequestTimeout(10_000)
                .setSocketTimeout(45_000)
                .build();
        return HttpClients.custom().setDefaultRequestConfig(config).build();
    }
}
