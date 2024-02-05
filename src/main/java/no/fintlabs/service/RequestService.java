package no.fintlabs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.config.RestClientConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import javax.annotation.Nullable;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestService {

    private final RestClient restClient;
    private final RestClientConfig restclientConfig;

    public Object getResource(String uri, String authorizationValue, String username) {
        log.debug("{}: Requesting url: {}{}", username, restclientConfig.getBaseUrl(), uri);
        return restClient.get()
                .uri(uri)
                .header(AUTHORIZATION, authorizationValue)
                .retrieve()
                .toEntity(Object.class)
                .getBody();
    }

    @Nullable
    public Object getCommonResource(String uri, String authorizationValue, String username) {
        try {
            log.debug("{}: Requesting url: {}{}", username, restclientConfig.getBaseUrl(), uri);
            return restClient.get()
                    .uri(uri)
                    .header(AUTHORIZATION, authorizationValue)
                    .retrieve()
                    .toEntity(Object.class)
                    .getBody();
        } catch (HttpClientErrorException.NotFound e) {
            log.debug("{}: 404 Not found", username);
            return null;
        }
    }

}
