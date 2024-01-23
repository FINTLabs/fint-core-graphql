package no.fintlabs.config;

import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLObjectType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.service.DataFetcherService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CodeRegistryConfig {

    private final DataFetcherService dataFetcherService;

    @Bean("codeRegistry")
    public GraphQLCodeRegistry codeRegistry(@Qualifier("query") GraphQLObjectType query) {
        GraphQLCodeRegistry.Builder builder = GraphQLCodeRegistry.newCodeRegistry();

        query.getFieldDefinitions().forEach(fieldDefinition -> {
            dataFetcherService.attachDataFetchers(builder, query, fieldDefinition);
        });

        return builder.build();
    }

}
