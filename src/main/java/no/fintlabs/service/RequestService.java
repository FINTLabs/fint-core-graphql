package no.fintlabs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestService {

    @Value("${fint.graphql.base-url:https://play-with-fint.felleskomponent.no}")
    private String baseUrl;

    private final RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();

    public Object getResource(String uri, String authorizationValue) {
        log.debug("Requesting url: {}{}", baseUrl, uri);
        return restClient.get()
                .uri(uri)
                .header(AUTHORIZATION, authorizationValue)
                .retrieve()
                .toEntity(Object.class)
                .getBody();
    }

}
