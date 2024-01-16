package no.fintlabs.config;

import graphql.Scalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.reflection.ReflectionService;
import no.fintlabs.reflection.model.FintMainObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class QueryConfig {

    private final ReflectionService reflectionService;

    @Bean
    public GraphQLObjectType buildQuery() {
        return GraphQLObjectType.newObject()
                .name("Query")
                .fields(getFieldDefinitions())
                .build();
    }

    private List<GraphQLFieldDefinition> getFieldDefinitions() {
        return reflectionService.getFintMainObjects().values().stream()
                .map(this::buildFieldDefinition)
                .collect(Collectors.toList());
    }

    private GraphQLFieldDefinition buildFieldDefinition(FintMainObject fintMainObject) {
        return GraphQLFieldDefinition.newFieldDefinition()
                .name(fintMainObject.getName())
                .arguments(buildArguments(fintMainObject))
                .type(Scalars.GraphQLString)
                .build();
    }

    private List<GraphQLArgument> buildArguments(FintMainObject fintMainObject) {
        return fintMainObject.getIdentificatorFields().stream()
                .map(field -> GraphQLArgument.newArgument()
                        .name(field)
                        .type(Scalars.GraphQLString)
                        .build())
                .collect(Collectors.toList());
    }

}
