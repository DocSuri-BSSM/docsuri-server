package com.example.docsuriserver.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GeminiConfig {

    @Bean
    public RestClient geminiRestClient(
            @Value("${app.gemini.endpoint}") String endpoint,
            @Value("${app.gemini.timeout-seconds:60}") int timeoutSeconds) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout(timeoutSeconds * 1000);

        return RestClient.builder()
                .baseUrl(endpoint)
                .requestFactory(requestFactory)
                .build();
    }
}
