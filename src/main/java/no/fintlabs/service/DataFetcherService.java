package no.fintlabs.service;

import graphql.GraphQLContext;
import graphql.schema.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.exceptions.MissingArgumentException;
import no.fintlabs.exceptions.MissingAuthorizationException;
import no.fintlabs.reflection.model.FintObject;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Service
@Slf4j
@RequiredArgsConstructor
public class DataFetcherService {

    private final static String DATA = "data";

    private final RequestService requestService;
    private final ReferenceService referenceService;

    public void attachDataFetchers(GraphQLCodeRegistry.Builder builder, GraphQLObjectType parentType, GraphQLFieldDefinition fieldDefinition) {
        if (fieldDefinition.getType() instanceof GraphQLObjectType objectType) {
            FintObject fintObject = referenceService.getFintObject(objectType.hashCode());
            if (fintObject.isMainObject()) {
                createDataFetcherForMainObject(builder, parentType, fieldDefinition, fintObject);
            }
            objectType.getFieldDefinitions().forEach(childFieldDefinition -> attachDataFetchers(
                    builder,
                    objectType,
                    childFieldDefinition)
            );
        } else {
            builder.dataFetcher(parentType, fieldDefinition, getDataFromGraphQLContext(fieldDefinition));
        }
    }

    private void createDataFetcherForMainObject(GraphQLCodeRegistry.Builder builder,
                                                GraphQLObjectType parentType,
                                                GraphQLFieldDefinition fieldDefinition,
                                                FintObject fintObject) {
        builder.dataFetcher(parentType, fieldDefinition, environment -> {
            setAuthorizationValueToContext(environment);
            return updateGraphQLContextData(environment, requestService.getResource(
                    createRequestUri(environment, fintObject),
                    environment.getGraphQlContext().get(AUTHORIZATION)
            ));
        });
    }

    private CompletableFuture<Object> updateGraphQLContextData(DataFetchingEnvironment environment, Mono<ResponseEntity<Object>> resource) {
        return resource.mapNotNull(responseEntity -> {
            environment.getGraphQlContext().put(DATA, responseEntity.getBody());
            return responseEntity.getBody();
        }).toFuture();
    }

    private DataFetcher<?> getDataFromGraphQLContext(GraphQLFieldDefinition fieldDefinition) {
        return env -> {
            Map<String, Object> data = env.getGraphQlContext().get(DATA);
            return data.get(fieldDefinition.getName());
        };
    }

    private String createRequestUri(DataFetchingEnvironment environment, FintObject fintObject) {
        Map.Entry<String, Object> firstArgument = getFirstArgument(environment);
        return String.format("%s/%s/%s", fintObject.getResourceUrl(), firstArgument.getKey(), firstArgument.getValue());
    }

    private Map.Entry<String, Object> getFirstArgument(DataFetchingEnvironment environment) {
        return environment.getArguments().entrySet().stream()
                .findFirst()
                .orElseThrow(MissingArgumentException::new);
    }

    private void setAuthorizationValueToContext(DataFetchingEnvironment environment) {
        if (environment.getGraphQlContext().hasKey(AUTHORIZATION)) {
            return;
        }
        environment.getGraphQlContext().put(AUTHORIZATION, getAuthorizationValue(getServerHttpRequest(environment.getGraphQlContext())));
    }

    private String getAuthorizationValue(ServerHttpRequest serverHttpRequest) {
        return Optional.ofNullable(serverHttpRequest.getHeaders().get(AUTHORIZATION))
                .map(List::getFirst)
                .orElseThrow(MissingAuthorizationException::new);
    }

    private ServerHttpRequest getServerHttpRequest(GraphQLContext ctx) {
        return Optional.ofNullable(ctx.<ServerWebExchange>get(ServerWebExchange.class))
                .map(ServerWebExchange::getRequest)
                .orElseThrow(() -> new RuntimeException("ServerWebExchange not found in GraphQLContext"));
    }

}
