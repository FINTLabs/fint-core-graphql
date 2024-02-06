package no.fintlabs.service;

import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.exception.exceptions.CacheNotFoundException;
import no.fintlabs.exception.exceptions.EntityNotFoundException;
import no.fintlabs.exception.exceptions.ForbiddenAccessException;
import no.fintlabs.exception.exceptions.UnexpectedErrorException;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import javax.annotation.Nullable;
import java.io.IOException;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestService {

    private final RestClient restClient;

    public Object getResource(String uri, DataFetchingEnvironment environment) {
        increaseCount(environment);
        return restClient.get()
                .uri(uri)
                .header(AUTHORIZATION, getAuthorizationValue(environment))
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .toEntity(Object.class)
                .getBody();
    }

    @Nullable
    public Object getCommonResource(String uri, DataFetchingEnvironment environment) {
        increaseCount(environment);
        try {
            return restClient.get()
                    .uri(uri)
                    .header(AUTHORIZATION, getAuthorizationValue(environment))
                    .retrieve()
                    .toEntity(Object.class)
                    .getBody();
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }

    private void handleError(HttpRequest request, ClientHttpResponse response) throws IOException {
        switch (response.getStatusCode()) {
            case NOT_FOUND -> throw new EntityNotFoundException();
            case FORBIDDEN -> throw new ForbiddenAccessException(request.getURI().toASCIIString());
            case INTERNAL_SERVER_ERROR -> handle5xxClientError(response);
            default -> throw new IllegalStateException("Unexpected value: " + response.getStatusCode());
        }
    }

    private void handle5xxClientError(ClientHttpResponse response) throws IOException {
        if (response.getStatusText().contains("CacheNotFoundException")) {
            throw new CacheNotFoundException();
        }
        throw new UnexpectedErrorException();
    }

    private String getAuthorizationValue(DataFetchingEnvironment environment) {
        return environment.getGraphQlContext().get(AUTHORIZATION);
    }

    private void increaseCount(DataFetchingEnvironment environment) {
        Integer count = environment.getGraphQlContext().get("counter");
        environment.getGraphQlContext().put("counter", count + 1);
    }

}
