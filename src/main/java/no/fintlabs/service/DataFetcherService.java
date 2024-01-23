package no.fintlabs.service;

import graphql.GraphQLContext;
import graphql.schema.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.exceptions.MissingArgumentException;
import no.fintlabs.exceptions.MissingAuthorizationException;
import no.fintlabs.reflection.model.FintObject;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Service
@Slf4j
@RequiredArgsConstructor
public class DataFetcherService {

    private final ReferenceService referenceService;

    public void attachNonQueryableDataFetcher(GraphQLCodeRegistry.Builder builder, GraphQLObjectType parentType, GraphQLFieldDefinition fieldDefinition) {
        if (fieldDefinition.getType() instanceof GraphQLObjectType objectType) {
            objectType.getFieldDefinitions().forEach(childFieldDefinition -> {
                attachNonQueryableDataFetcher(builder, objectType, childFieldDefinition);
            });
        }
        // If it's a normal Java class String, int, float, etc...
        builder.dataFetcher(parentType, fieldDefinition, getDataFromGraphQLContext(fieldDefinition));
    }

    private DataFetcher<?> getDataFromGraphQLContext(GraphQLFieldDefinition fieldDefinition) {
        return env -> {
            Map<String, Object> data =  env.getGraphQlContext().get("data");
            return "Test";
        };
    }

    public void attachQueryableDataFetcher(GraphQLCodeRegistry.Builder builder, GraphQLObjectType query, GraphQLFieldDefinition queryableFieldDefinition) {
        FintObject fintObject = referenceService.getFintObject(queryableFieldDefinition.getType().hashCode());
        // TODO: Handle felles resource differently
        builder.dataFetcher(query, queryableFieldDefinition, environment -> {
            setAuthorizationValueToContext(environment);
            String requestUri = createRequestUri(environment, fintObject);
            log.info("Url: {}", requestUri);
            return "ok";
        });
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
