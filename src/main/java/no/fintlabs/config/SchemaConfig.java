package no.fintlabs.config;

import graphql.GraphQL;
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
    public GraphQL graphQL(GraphQLSchema graphQLSchema) {
        return GraphQL.newGraphQL(graphQLSchema).build();
    }

    @Bean
    public GraphQLSchema graphQLSchema(@Qualifier("query") GraphQLObjectType query,
                                       @Qualifier("additionalTypes") Set<GraphQLType> additionalTypes) {
        return GraphQLSchema.newSchema()
                .query(query)
                .additionalTypes(additionalTypes)
                .build();
    }

}
