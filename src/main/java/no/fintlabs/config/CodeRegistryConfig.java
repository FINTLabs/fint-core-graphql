package no.fintlabs.config;

import graphql.GraphQLContext;
import graphql.schema.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.ReferenceService;
import no.fintlabs.RequestService;
import no.fintlabs.service.ReferenceService;
import no.fintlabs.exceptions.MissingArgumentException;
import no.fintlabs.exceptions.MissingAuthorizationException;
import no.fintlabs.reflection.model.FintObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CodeRegistryConfig {


    private final ReferenceService referenceService;
    private final RequestService requestService;
    private final GraphQLCodeRegistry.Builder builder = GraphQLCodeRegistry.newCodeRegistry();

    @Bean("codeRegistry")
    public GraphQLCodeRegistry codeRegistry(@Qualifier("query") GraphQLObjectType query)  {
        query.getFieldDefinitions().forEach(fieldDefinition -> {
            attachQueryableDataFetcher(query, fieldDefinition);

        });
        return builder.build();
    }

    private void attachQueryableDataFetcher(GraphQLObjectType query, GraphQLFieldDefinition queryableFieldDefinition) {
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

    private void recursiveFunction(GraphQLFieldDefinition fieldDefinition) {
        if (referenceService.containsFintObject(fieldDefinition.getType().hashCode())){
            FintObject fintObject = referenceService.getFintObject(fieldDefinition.getType().hashCode());
            if (fintObject.isMainObject()) {
                if (fintObject.getDomainName().equalsIgnoreCase("felles")) {
                    // TODO: Handle relasjoner
                    // If first call
                    // Else get link from data
                } else {

                }
            } else {

            }
        }
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
