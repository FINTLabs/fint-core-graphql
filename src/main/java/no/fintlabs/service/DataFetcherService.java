package no.fintlabs.service;

import graphql.schema.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.exception.exceptions.MissingArgumentException;
import no.fintlabs.exception.exceptions.MissingLinkException;
import no.fintlabs.reflection.ReflectionService;
import no.fintlabs.reflection.model.FintObject;
import no.fintlabs.reflection.model.FintRelation;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Service
@Slf4j
@RequiredArgsConstructor
public class DataFetcherService {

    private final static String LINKS = "_links";
    private final static String HREF = "href";

    private final RequestService requestService;
    private final ReferenceService referenceService;
    private final ReflectionService reflectionService;
    private final ContextService contextService;

    public void attachDataFetchers(GraphQLCodeRegistry.Builder builder,
                                   GraphQLObjectType parentType,
                                   GraphQLFieldDefinition fieldDefinition) {
        FintObject fintObject = referenceService.getFintObject(fieldDefinition.getType().hashCode());
        createDataFetcher(builder, parentType, fieldDefinition, fintObject);
        fintObject.getRelations().forEach(fintRelation -> {
            createRelationDataFetcher(builder, fieldDefinition.getType(), fintRelation);
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
        FintObject fintObject = reflectionService.getFintObject(fintRelation.packageName());
        String fieldName = fintRelation.relationName().toLowerCase();

        return environment -> switch (fintRelation.multiplicity()) {
            case ONE_TO_MANY, ZERO_TO_MANY -> getFintRelationResources(environment, fintObject, fieldName);
            default -> getFintRelationResource(environment, fintObject, fieldName);
        };
    }

    private void createDataFetcher(GraphQLCodeRegistry.Builder builder,
                                   GraphQLObjectType parentType,
                                   GraphQLFieldDefinition fieldDefinition,
                                   FintObject fintObject) {
        builder.dataFetcher(parentType, fieldDefinition, environment -> {
            contextService.checkIfUserIsBlocked(environment);
            contextService.setAuthorizationValueToContext(environment);

            if (fintObject.getDomainName().equalsIgnoreCase("felles")) {
                // TODO: CT-1158 Handle felles resources
            }
            return getFintResource(environment, fintObject);
        });
    }

    private Object getFintRelationResource(DataFetchingEnvironment environment, FintObject fintObject, String fieldName) {
        log.info("Getting one resource");
        return getRelationRequestUri(environment, fintObject, fieldName).stream()
                .map(link -> requestService.getResource(
                        link,
                        environment.getGraphQlContext().get(AUTHORIZATION))
                ).toList().getFirst();
    }

    private List<Object> getFintRelationResources(DataFetchingEnvironment environment, FintObject fintObject, String fieldName) {
        log.info("Getting many resources");
        return getRelationRequestUri(environment, fintObject, fieldName).stream()
                .map(link -> requestService.getResource(
                        link,
                        environment.getGraphQlContext().get(AUTHORIZATION))
                ).toList();
    }

    private Object getFintResource(DataFetchingEnvironment environment, FintObject fintObject) {
        return requestService.getResource(
                createRequestUri(environment, fintObject),
                environment.getGraphQlContext().get(AUTHORIZATION)
        );
    }

    private List<String> getRelationRequestUri(DataFetchingEnvironment environment, FintObject fintObject, String fieldName) {
        Map<String, Map<String, List<Map<String, String>>>> source = environment.getSource();
        Map<String, List<Map<String, String>>> linksMap = source.get(LINKS);

        if (linksMap == null || !linksMap.containsKey(fieldName) || linksMap.get(fieldName).isEmpty()) {
            throw new MissingLinkException(fieldName);
        }

        return linksMap.get(fieldName).stream()
                .map(map -> map.get(HREF))
                .toList();
    }

    private String createRequestUri(DataFetchingEnvironment environment, FintObject fintObject) {
        Map.Entry<String, Object> firstArgument = getFirstArgument(environment);
        return String.format("%s/%s/%s", fintObject.getResourceUrl(), firstArgument.getKey(), firstArgument.getValue());
    }

    private Map.Entry<String, Object> getFirstArgument(DataFetchingEnvironment environment) {
        return environment.getArguments().entrySet().stream()
                .findFirst()
                .orElseThrow(MissingArgumentException::new);
    }

}
