package config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GroqConfig {

    @Bean
    public RestClient groqRestClient(
            RestClient.Builder builder,
            @Value("${groq.base-url}") String baseUrl,
            @Value("${groq.api-key:}") String apiKey
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        RestClient.Builder groqBuilder = builder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_JSON_VALUE
                );

        if (apiKey != null && !apiKey.isBlank()) {
            groqBuilder.defaultHeader(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + apiKey
            );
        }

        return groqBuilder.build();
    }
}