package no.fintlabs.service;

import graphql.schema.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.reflection.model.FintObject;
import no.fintlabs.reflection.model.FintRelation;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DataFetcherService {

    private final ReferenceService referenceService;
    private final ContextService contextService;
    private final ResourceFetcher resourceFetcher;

    public void attachDataFetchers(GraphQLCodeRegistry.Builder builder,
                                   GraphQLObjectType parentType,
                                   GraphQLFieldDefinition fieldDefinition) {
        FintObject fintObject = referenceService.getFintObject(fieldDefinition.getType().hashCode());
        createDataFetcher(builder, parentType, fieldDefinition, fintObject);
        fintObject.getRelations().forEach(fintRelation -> {
            createRelationDataFetcher(builder, fieldDefinition.getType(), fintRelation);
        });
    }

    private void createDataFetcher(GraphQLCodeRegistry.Builder builder,
                                   GraphQLObjectType parentType,
                                   GraphQLFieldDefinition fieldDefinition,
                                   FintObject fintObject) {
        builder.dataFetcher(parentType, fieldDefinition, environment -> {
            contextService.checkIfUserIsBlocked(environment);
            contextService.setAuthorizationValueToContext(environment);

            if (fintObject.getDomainName().equalsIgnoreCase("felles")) {
                return resourceFetcher.getCommonFintResource(environment, fintObject);
            }
            return resourceFetcher.getFintResource(environment, fintObject);
        });
    }

    private void createRelationDataFetcher(GraphQLCodeRegistry.Builder builder,
                                           GraphQLOutputType parentType,
                                           FintRelation fintRelation) {
        builder.dataFetcher(FieldCoordinates.coordinates(
                        (GraphQLObjectType) parentType,
                        fintRelation.relationName().toLowerCase()),
                createDataFetcher(fintRelation)
        );
    }

    private DataFetcher<?> createDataFetcher(FintRelation fintRelation) {
        String fieldName = fintRelation.relationName().toLowerCase();

        return environment -> switch (fintRelation.multiplicity()) {
            case ONE_TO_MANY, ZERO_TO_MANY -> resourceFetcher.getFintRelationResources(environment, fieldName);
            default -> resourceFetcher.getFintRelationResource(environment, fieldName);
        };
    }

}
