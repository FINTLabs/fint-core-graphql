package no.fintlabs.service;

import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.exception.exceptions.CacheNotFoundException;
import no.fintlabs.exception.exceptions.EntityNotFoundException;
import no.fintlabs.exception.exceptions.ForbiddenAccessException;
import no.fintlabs.exception.exceptions.UnexpectedErrorException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestService {

    private final WebClient webClient;

    public CompletableFuture<Object> getResource(String uri, DataFetchingEnvironment environment) {
        increaseCount(environment);
        return webClient.get()
                .uri(uri)
                .header(AUTHORIZATION, getAuthorizationValue(environment))
                .retrieve()
//                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToMono(Object.class)
                .toFuture();
    }

    public Object getRelationResource(String uri, DataFetchingEnvironment environment) {
        increaseCount(environment);
        try {
            return webClient.get()
                    .uri(uri)
                    .header(AUTHORIZATION, getAuthorizationValue(environment))
                    .retrieve()
                    .toEntity(Object.class);
        } catch (HttpClientErrorException clientErrorException) {
            return null;
        } catch (HttpServerErrorException serverErrorException) {
            log.error("Server error when accessing resource: " + uri, serverErrorException);
            throw new RuntimeException("Server error when accessing resource: " + uri, serverErrorException);
        }
    }

    @Nullable
    public Object getCommonResource(String uri, DataFetchingEnvironment environment) {
        increaseCount(environment);
        try {
            return webClient.get()
                    .uri(uri)
                    .header(AUTHORIZATION, getAuthorizationValue(environment))
                    .retrieve()
                    .toEntity(Object.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }

    private void handleError(HttpRequest request, ClientHttpResponse response) throws IOException {
        switch (response.getStatusCode()) {
            case NOT_FOUND -> throw new EntityNotFoundException();
            case FORBIDDEN -> throw new ForbiddenAccessException(request.getURI().toASCIIString());
            case INTERNAL_SERVER_ERROR -> handle5xxClientError(response, request);
            default -> throw new IllegalStateException("Unexpected value: " + response.getStatusCode());
        }
    }

    private void handle5xxClientError(ClientHttpResponse response, HttpRequest httpRequest) throws IOException {
        if (response.getStatusText().contains("CacheNotFoundException")) {
            throw new CacheNotFoundException(httpRequest.getURI().toASCIIString());
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
