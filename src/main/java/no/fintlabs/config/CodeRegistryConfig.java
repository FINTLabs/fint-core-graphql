package no.fintlabs.config;

import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLObjectType;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.reflection.model.FintObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Slf4j
@Configuration
public class CodeRegistryConfig {

    @Value("${fint.graphql.base-url:https://play-with-fint.felleskomponent.no}")
    private String baseUrl;

    @Bean
    public GraphQLCodeRegistry codeRegistry(@Qualifier("query") GraphQLObjectType query,
                                            Map<Integer, FintObject> fintObjectRelation) {
        query.getFieldDefinitions().forEach(fieldDefinition -> {
            FintObject fintObject = fintObjectRelation.get(fieldDefinition.getType().hashCode());
            log.info(baseUrl + fintObject.getComponentUrl() + fintObject.getResourceUrl());

        });

        return GraphQLCodeRegistry.newCodeRegistry().build();
    }

}
