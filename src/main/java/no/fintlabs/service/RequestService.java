package no.fintlabs.service;

import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public Object getResource(String uri, DataFetchingEnvironment environment) {
        increaseCount(environment);
        return restClient.get()
                .uri(uri)
                .header(AUTHORIZATION, getAuthorizationValue(environment))
                .retrieve()
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

    private String getAuthorizationValue(DataFetchingEnvironment environment) {
        return environment.getGraphQlContext().get(AUTHORIZATION);
    }

    private void increaseCount(DataFetchingEnvironment environment) {
        Integer count = environment.getGraphQlContext().get("counter");
        environment.getGraphQlContext().put("counter", count + 1);
    }

}
