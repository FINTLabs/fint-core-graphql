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
    private final static String LINKS = "_links";

    private final RequestService requestService;
    private final ReferenceService referenceService;
    
    public void attachDataFetchers(GraphQLCodeRegistry.Builder builder, GraphQLObjectType parentType, GraphQLFieldDefinition fieldDefinition) {
        GraphQLObjectType objectType = (GraphQLObjectType) fieldDefinition.getType();
        FintObject fintObject = referenceService.getFintObject(objectType.hashCode());
        attachDataFetcherForQueryableObjects(builder, parentType, fieldDefinition, fintObject);
        log.info("Fetching thing for: {}", fintObject.getName());
        objectType.getFieldDefinitions().forEach(childFieldDefinition -> {
            createDataFetchers(builder, objectType, childFieldDefinition);
        });
    }

    public void createDataFetchers(GraphQLCodeRegistry.Builder builder, GraphQLObjectType parentType, GraphQLFieldDefinition fieldDefinition) {
        if (parentType.getName().equalsIgnoreCase("elev")) {
            log.info(fieldDefinition.getName());
        }
        if (fieldDefinition.getType() instanceof GraphQLObjectType objectType) {
            FintObject fintObject = referenceService.getFintObject(objectType.hashCode());
            if (fintObject.isMainObject()) {
                attachDataFetcherForRelation(builder, parentType, fieldDefinition);
            }
            objectType.getFieldDefinitions().forEach(childFieldDefinition -> createDataFetchers(
                    builder,
                    objectType,
                    childFieldDefinition)
            );
        } else {
            builder.dataFetcher(parentType, fieldDefinition, getDataFromGraphQLContext(fieldDefinition));
        }
    }
    
    private void attachDataFetcherForRelation(GraphQLCodeRegistry.Builder builder,
                                              GraphQLObjectType parentType,
                                              GraphQLFieldDefinition fieldDefinition) {
        builder.dataFetcher(parentType, fieldDefinition, environment -> {
            return updateGraphQLContextData(environment, requestService.getResource(
                    getRelationRequestUri(environment, parentType),
                    environment.getGraphQlContext().get(AUTHORIZATION)
            ));
        });
    }

    private String getRelationRequestUri(DataFetchingEnvironment environment, GraphQLObjectType parentType) {
        Map<String, Object> contextData = environment.getGraphQlContext().get(DATA);
        if (contextData.get(LINKS) instanceof Map<?,?> linksMap) {
            Object linksObject = linksMap.get(parentType.getName().toLowerCase());

            if (linksObject instanceof List<?>) {
                List<Map<String, String>> linksList = (List<Map<String, String>>) linksObject;
                if (!linksList.isEmpty() && linksList.get(0).containsKey("href")) {
                    String s = linksList.get(0).get("href");
                    log.info("LINK: {}", s);
                    return s;
                }
            }
        }
        log.error("CANT FIND HREF");
        return null;
    }

    private void attachDataFetcherForQueryableObjects(GraphQLCodeRegistry.Builder builder,
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
