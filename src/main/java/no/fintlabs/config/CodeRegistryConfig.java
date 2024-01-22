package no.fintlabs.config;

import graphql.GraphQLContext;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLObjectType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final ReflectionService reflectionService;
    private final RequestService requestService;

    @Bean("codeRegistry")
    public GraphQLCodeRegistry codeRegistry(GraphQLObjectType query)  {
        GraphQLCodeRegistry.Builder builder = GraphQLCodeRegistry.newCodeRegistry();



        return builder.build();
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
