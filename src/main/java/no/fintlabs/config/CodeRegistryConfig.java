package no.fintlabs.config;

import graphql.GraphQLContext;
import graphql.schema.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.ReferenceService;
import no.fintlabs.RequestService;
import no.fintlabs.exceptions.MissingAuthorizationException;
import no.fintlabs.reflection.ReflectionService;
import no.fintlabs.reflection.model.FintObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
        query.getFieldDefinitions().forEach(this::recursiveFunction);
        return builder.build();
    }

    private void recursiveFunction(GraphQLFieldDefinition fieldDefinition) {
        if (referenceService.containsFintObject(fieldDefinition.getType().hashCode())){
            FintObject fintObject = referenceService.getFintObject(fieldDefinition.getType().hashCode());
            if (fintObject.isMainObject()) {
                // TODO: Handle relasjoner
                // If first call
                // Else get link from data
            } else {
            }
        }
    }

    private String getAuthorizationValue(ServerHttpRequest request) {
        return Optional.ofNullable(request.getHeaders().get(AUTHORIZATION))
                .map(List::getFirst)
                .orElseThrow(MissingAuthorizationException::new);
    }

    private ServerHttpRequest getServerHttpRequest(GraphQLContext ctx) {
        return Optional.ofNullable(ctx.<ServerWebExchange>get(ServerWebExchange.class))
                .map(ServerWebExchange::getRequest)
                .orElseThrow(() -> new RuntimeException("ServerWebExchange not found in GraphQLContext"));
    }

}
