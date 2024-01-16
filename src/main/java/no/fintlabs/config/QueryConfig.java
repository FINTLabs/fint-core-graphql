package no.fintlabs.config;

import graphql.Scalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.reflection.ReflectionService;
import no.fintlabs.reflection.model.FintMainObject;
import no.fintlabs.reflection.model.FintObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class QueryConfig {

    private final Map<String, GraphQLObjectType> processedTypes = new HashMap<>();
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
                .type(getOrCreateObjectType(fintMainObject))
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

    public GraphQLObjectType getOrCreateObjectType(FintObject fintObject) {
        String packageName = fintObject.getPackageName();
        if (processedTypes.containsKey(packageName)) {
            log.info("Using processed type: {}", fintObject.getName());
            return processedTypes.get(packageName);
        }

        GraphQLObjectType objectType = createObjectType(fintObject);
        processedTypes.put(packageName, objectType);
        return objectType;
    }

    private GraphQLObjectType createObjectType(FintObject fintObject) {
        log.info("Creating object type: {}", fintObject.getName());
        GraphQLObjectType.Builder objectTypeBuilder = GraphQLObjectType.newObject()
                .name(fintObject.getName());

        addFields(fintObject, objectTypeBuilder);

        return objectTypeBuilder.build();
    }

    private void addFields(FintObject fintObject, GraphQLObjectType.Builder objectTypeBuilder) {
        fintObject.getFields().forEach(field -> {
            GraphQLFieldDefinition.Builder fieldBuilder = GraphQLFieldDefinition.newFieldDefinition()
                    .name(field.getName());

            if (typeIsFromJava(field.getType())) {
                // TODO: Handle different Java types
                objectTypeBuilder.field(fieldBuilder.type(Scalars.GraphQLString).build());
            } else {
                objectTypeBuilder.field(fieldBuilder.type(getOrCreateObjectType(reflectionService.findFintObject(field.getType().getName()))));
            }
        });
    }

    private boolean typeIsFromJava(Class<?> clazz) {
        return clazz.getClassLoader() == null || clazz.getPackage().getName().startsWith("java") || clazz.getPackage().getName().startsWith("javax");
    }

}
