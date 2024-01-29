package no.fintlabs.config;

import graphql.GraphQL;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class SchemaConfig {

    @Bean
    public GraphQL graphQL(GraphQLSchema graphQLSchema, DataFetcherExceptionHandler dataFetcherExceptionHandler) {
        return GraphQL.newGraphQL(graphQLSchema)
                .defaultDataFetcherExceptionHandler(dataFetcherExceptionHandler)
                .build();
    }

    @Bean
    public GraphQLSchema graphQLSchema(@Qualifier("query") GraphQLObjectType query,
                                       @Qualifier("additionalTypes") Set<GraphQLType> additionalTypes,
                                       @Qualifier("codeRegistry")GraphQLCodeRegistry codeRegistry) {
        return GraphQLSchema.newSchema()
                .query(query)
                .additionalTypes(additionalTypes)
                .codeRegistry(codeRegistry)
                .build();
    }

}
