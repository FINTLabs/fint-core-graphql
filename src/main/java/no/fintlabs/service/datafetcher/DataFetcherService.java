package no.fintlabs.service.datafetcher;

import graphql.schema.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.reflection.model.FintObject;
import no.fintlabs.reflection.model.FintRelation;
import no.fintlabs.service.ReferenceService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DataFetcherService {

    private final ReferenceService referenceService;
    private final ContextService contextService;
    private final ResourceFetcher resourceFetcher;

    public void attachDataFetchers(GraphQLCodeRegistry.Builder builder,
                                   GraphQLObjectType querySchema,
                                   GraphQLFieldDefinition fieldDefinition) {
        FintObject fintObject = referenceService.getFintObject(fieldDefinition.getType().hashCode());
        createFintResourceDataFetcher(builder, querySchema, fieldDefinition, fintObject);
        fintObject.getRelations().forEach(fintRelation ->
                createRelationDataFetcher(builder, fieldDefinition.getType(), fintRelation)
        );
    }

    private void createFintResourceDataFetcher(GraphQLCodeRegistry.Builder builder,
                                               GraphQLObjectType querySchema,
                                               GraphQLFieldDefinition fieldDefinition,
                                               FintObject fintObject) {
        builder.dataFetcher(querySchema, fieldDefinition, environment -> {
            contextService.checkIfUserIsBlocked(environment);
            contextService.setAuthorizationValueToContext(environment);

            if (fintObject.getDomainName().equalsIgnoreCase("felles")) { // Asks multiple consumers for the same resource
                return resourceFetcher.getCommonFintResources(environment, fintObject).toFuture();
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
                determineFetchingStyle(fintRelation)
        );
    }

    private DataFetcher<?> determineFetchingStyle(FintRelation fintRelation) {
        String fieldName = fintRelation.relationName().toLowerCase();

        return environment -> switch (fintRelation.multiplicity()) {
            case ONE_TO_MANY, ZERO_TO_MANY -> resourceFetcher.getFintRelationResources(environment, fieldName)
                    .collectList()
                    .toFuture();
            default -> resourceFetcher.getFintRelationResource(environment, fieldName).toFuture();
        };
    }

}
