package org.example.thoughts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

@SpringBootApplication
public class ThoughtsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThoughtsServiceApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public CloseableHttpClient httpClient() {
        return HttpClients.createDefault();
    }
}