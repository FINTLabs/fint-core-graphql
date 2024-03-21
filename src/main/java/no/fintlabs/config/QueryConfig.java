package no.fintlabs.config;

import graphql.Scalars;
import graphql.schema.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fint.model.FintMainObject;
import no.fintlabs.reflection.ReflectionService;
import no.fintlabs.reflection.model.FintObject;
import no.fintlabs.reflection.model.FintRelation;
import no.fintlabs.service.ReferenceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class QueryConfig {

    private final ReferenceService referenceService;
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
                .name(fintObject.getName().toLowerCase())
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

        GraphQLObjectType graphQLObjectType = objectTypeBuilder.build();
        referenceService.addReferenecs(fintObject, graphQLObjectType);

        return graphQLObjectType;
    }

    private void addRelations(FintObject fintObject, GraphQLObjectType.Builder objectTypeBuilder) {
        fintObject.getRelations().forEach(relation -> {
            FintObject relationFintObject = reflectionService.getFintObject(relation.packageName());
            if (!relationFintObject.isAbstract())
                objectTypeBuilder.field(GraphQLFieldDefinition.newFieldDefinition()
                        .name(relation.relationName().toLowerCase())
                        .type(getRelationType(relation, relationFintObject))
                        .build());
        });
    }

    private GraphQLOutputType getRelationType(FintRelation relation, FintObject relationFintObject) {
        GraphQLOutputType graphQLOutputType = determineGraphQLOutputType(relationFintObject);
        return switch (relation.multiplicity()) {
            case ONE_TO_ONE -> GraphQLNonNull.nonNull(graphQLOutputType);
            case ZERO_TO_MANY -> GraphQLList.list(graphQLOutputType);
            case ONE_TO_MANY ->
                    GraphQLNonNull.nonNull(GraphQLList.list(graphQLOutputType));
            default -> graphQLOutputType;
        };
    }

    private GraphQLOutputType determineGraphQLOutputType(FintObject relationFintObject) {
        if (relationFintObject.isReference()) {
            return Scalars.GraphQLString;
        } else {
            return GraphQLTypeReference.typeRef(relationFintObject.getName());
        }
    }

    private void addFields(FintObject fintObject, GraphQLObjectType.Builder objectTypeBuilder) {
        fintObject.getFields().forEach(field ->
                objectTypeBuilder.field(
                        GraphQLFieldDefinition.newFieldDefinition()
                                .name(field.getName())
                                .type(determineGraphQLType(field))
                )
        );
    }

    private GraphQLOutputType determineGraphQLType(Field field) {
        Class<?> fieldType = field.getType();
        if (List.class.isAssignableFrom(fieldType)) {
            return determineGraphQLListType(field);
        } else {
            return determineScalarGraphQLType(fieldType);
        }
    }

    private GraphQLOutputType determineGraphQLListType(Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType parameterizedType) {
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            for (Type actualTypeArgument : actualTypeArguments) {
                if (actualTypeArgument instanceof Class<?> fieldArgClass) {
                    if (FintMainObject.class.isAssignableFrom(fieldArgClass)) {
                        return GraphQLList.list(getOrCreateObjectType(reflectionService.getFintObject(fieldArgClass.getName())));
                    } else {
                        return GraphQLList.list(determineScalarGraphQLType(fieldArgClass));
                    }
                }
            }
        }
        return null;
    }

    private GraphQLOutputType determineScalarGraphQLType(Class<?> clazz) {
        if (no.fint.model.FintObject.class.isAssignableFrom(clazz)) {
            return getOrCreateObjectType(reflectionService.getFintObject(clazz.getName()));
        } else if (Boolean.class.isAssignableFrom(clazz) || boolean.class.isAssignableFrom(clazz)) {
            return Scalars.GraphQLBoolean;
        } else if (Float.class.isAssignableFrom(clazz) || float.class.isAssignableFrom(clazz) || Double.class.isAssignableFrom(clazz) || double.class.isAssignableFrom(clazz)) {
            return Scalars.GraphQLFloat;
        } else if (Integer.class.isAssignableFrom(clazz) || int.class.isAssignableFrom(clazz) || Long.class.isAssignableFrom(clazz) || long.class.isAssignableFrom(clazz)) {
            return Scalars.GraphQLInt;
        } else {
            return Scalars.GraphQLString;
        }
    }

}
