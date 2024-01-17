package no.fintlabs.config;

import graphql.Scalars;
import graphql.schema.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.reflection.ReflectionService;
import no.fintlabs.reflection.model.FintObject;
import no.fintlabs.reflection.model.FintRelation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class QueryConfig {

    private final Map<String, GraphQLObjectType> processedTypes = new HashMap<>();
    private final ReflectionService reflectionService;

    @Bean("query")
    public GraphQLObjectType buildQuery() {
        return GraphQLObjectType.newObject()
                .name("Query")
                .fields(getFieldDefinitions())
                .build();
    }

    @Bean("additionalTypes")
    public Set<GraphQLType> buildAdditionalTypes() {
        return reflectionService.getFintObjects().values().stream()
                .filter(FintObject::shouldBeProcessed)
                .filter(fintObject -> !fintObject.isMainObject())
                .map(this::getOrCreateObjectType)
                .collect(Collectors.toSet());
    }

    private List<GraphQLFieldDefinition> getFieldDefinitions() {
        return reflectionService.getFintObjects().values().stream()
                .filter(FintObject::isMainObject)
                .filter(FintObject::shouldBeProcessed)
                .map(this::buildFieldDefinition)
                .collect(Collectors.toList());
    }

    private GraphQLFieldDefinition buildFieldDefinition(FintObject fintObject) {
        return GraphQLFieldDefinition.newFieldDefinition()
                .name(fintObject.getName())
                .arguments(buildArguments(fintObject))
                .type(getOrCreateObjectType(fintObject))
                .build();
    }

    private List<GraphQLArgument> buildArguments(FintObject fintObject) {
        return fintObject.getIdentificatorFields().stream()
                .map(field -> GraphQLArgument.newArgument()
                        .name(field)
                        .type(Scalars.GraphQLString)
                        .build())
                .collect(Collectors.toList());
    }

    public GraphQLObjectType getOrCreateObjectType(FintObject fintObject) {
        String packageName = fintObject.getPackageName();
        if (processedTypes.containsKey(packageName)) {
            return processedTypes.get(packageName);
        }

        GraphQLObjectType objectType = createObjectType(fintObject);
        processedTypes.put(packageName, objectType);
        return objectType;
    }

    private GraphQLObjectType createObjectType(FintObject fintObject) {
        GraphQLObjectType.Builder objectTypeBuilder = GraphQLObjectType.newObject()
                .name(fintObject.getName());

        addFields(fintObject, objectTypeBuilder);
        addRelations(fintObject, objectTypeBuilder);

        return objectTypeBuilder.build();
    }

    private void addRelations(FintObject fintObject, GraphQLObjectType.Builder objectTypeBuilder) {
        fintObject.getRelations().forEach(relation -> {
            if (!relationIsEmpty(relation)) {
                objectTypeBuilder.field(GraphQLFieldDefinition.newFieldDefinition()
                        .name(relation.relationName().toLowerCase())
                        .type(GraphQLTypeReference.typeRef(getFintObject(relation.packageName()).getName()))
                        .build());
            }
        });
    }

    private boolean relationIsEmpty(FintRelation relation) {
        FintObject fintObject = getFintObject(relation.packageName());
        if (fintObject.getFields().isEmpty()) {
            return fintObject.getRelations().isEmpty();
        }
        return false;
    }

    private void addFields(FintObject fintObject, GraphQLObjectType.Builder objectTypeBuilder) {
        fintObject.getFields().forEach(field -> {
            GraphQLFieldDefinition.Builder fieldBuilder = GraphQLFieldDefinition.newFieldDefinition()
                    .name(field.getName());

            if (typeIsFromJava(field.getType())) {
                objectTypeBuilder.field(fieldBuilder.type(determineGraphQLType(field.getType())).build());
            } else {
                objectTypeBuilder.field(fieldBuilder.type(getOrCreateObjectType(getFintObject(field.getType().getName()))));
            }
        });
    }

    private GraphQLScalarType determineGraphQLType(Class<?> fieldType) {
        if (Boolean.class.isAssignableFrom(fieldType) || boolean.class.isAssignableFrom(fieldType)) {
            return Scalars.GraphQLBoolean;
        } else if (Float.class.isAssignableFrom(fieldType) || float.class.isAssignableFrom(fieldType)) {
            return Scalars.GraphQLFloat;
        } else if (Integer.class.isAssignableFrom(fieldType) || int.class.isAssignableFrom(fieldType)) {
            return Scalars.GraphQLInt;
        } else {
            return Scalars.GraphQLString;
        }
    }

    private boolean typeIsFromJava(Class<?> clazz) {
        return clazz.getClassLoader() == null || clazz.getPackage().getName().startsWith("java") || clazz.getPackage().getName().startsWith("javax");
    }

    private FintObject getFintObject(String name) {
        if (reflectionService.getFintObjects().containsKey(name)) {
            return reflectionService.getFintObjects().get(name);
        } else {
            throw new RuntimeException("Could not find FintObject with name " + name);
        }
    }

}
