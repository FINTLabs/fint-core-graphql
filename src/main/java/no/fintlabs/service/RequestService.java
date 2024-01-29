package no.fintlabs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestService {

    private final RestClient restClient;

    public Object getResource(String uri, String authorizationValue) {
        return restClient.get()
                .uri(uri)
                .header(AUTHORIZATION, authorizationValue)
                .retrieve()
                .toEntity(Object.class)
                .getBody();
    }

}
