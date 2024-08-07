package no.fintlabs.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Getter
@Configuration
public class WebClientConfig {

    @Value("${fint.graphql.base-url:https://play-with-fint.felleskomponent.no}")
    private String baseUrl;

    @Bean
    public WebClient restClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

}
