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
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToMono(Object.class)
                .toFuture();
    }

    public Mono<Object> getRelationResource(String uri, DataFetchingEnvironment environment) {
        increaseCount(environment);
        try {
            return webClient.get()
                    .uri(uri)
                    .header(AUTHORIZATION, getAuthorizationValue(environment))
                    .retrieve()
                    .bodyToMono(Object.class);
        } catch (HttpClientErrorException clientErrorException) {
            return null;
        } catch (HttpServerErrorException serverErrorException) {
            log.error("Server error when accessing resource: " + uri, serverErrorException);
            throw new RuntimeException("Server error when accessing resource: " + uri, serverErrorException);
        }
    }

    @Nullable
    public Mono<Object> getCommonResource(String uri, DataFetchingEnvironment environment) {
        increaseCount(environment);
        log.info("Doing request to: {}", uri);
        try {
            return webClient.get()
                    .uri(uri)
                    .header(AUTHORIZATION, getAuthorizationValue(environment))
                    .retrieve()
                    .bodyToMono(Object.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }

    private Mono<? extends Throwable> handleError(ClientResponse response) {
        return response.statusCode().isError() ? switch (response.statusCode()) {
            case NOT_FOUND -> Mono.error(new EntityNotFoundException());
            case FORBIDDEN -> Mono.error(new ForbiddenAccessException(response.request().getURI().toASCIIString()));
            case INTERNAL_SERVER_ERROR -> handle5xxClientError(response);
            default -> Mono.error(new IllegalStateException("Unexpected value: " + response.statusCode()));
        } : Mono.empty();
    }

    private Mono<? extends Throwable> handle5xxClientError(ClientResponse response) {
        return response.bodyToMono(String.class)
                .flatMap(body -> {
                    if (body.contains("CacheNotFoundException")) {
                        return Mono.error(new CacheNotFoundException(response.request().getURI().toASCIIString()));
                    }
                    return Mono.error(new UnexpectedErrorException());
                });
    }

    private String getAuthorizationValue(DataFetchingEnvironment environment) {
        return environment.getGraphQlContext().get(AUTHORIZATION);
    }

    private void increaseCount(DataFetchingEnvironment environment) {
        Integer count = environment.getGraphQlContext().get("counter");
        environment.getGraphQlContext().put("counter", count + 1);
    }

}
