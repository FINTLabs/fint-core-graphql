package no.fintlabs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Service
@RequiredArgsConstructor
public class RequestService {

    private final WebClient webClient;

    public Mono<ResponseEntity<Object>> getResource(String uri, String authorizationValue) {
        return webClient.get()
                .uri(uri)
                .header(AUTHORIZATION, authorizationValue)
                .retrieve()
                .toEntity(Object.class);
    }

}
