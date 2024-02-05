package no.fintlabs.service;

import graphql.schema.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.config.RestClientConfig;
import no.fintlabs.core.resource.server.security.authentication.CorePrincipal;
import no.fintlabs.exception.exceptions.MissingLinkException;
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

    private final EndpointService endpointService;
    private final RequestService requestService;
    private final ReferenceService referenceService;
    private final ContextService contextService;
    private final RestClientConfig restClientConfig;

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
                return getCommonFintResource(environment, fintObject);
                // TODO: CT-1158 Handle felles resources
            }
            return getFintResource(environment, fintObject);
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
            case ONE_TO_MANY, ZERO_TO_MANY -> getFintRelationResources(environment, fieldName);
            default -> getFintRelationResource(environment, fieldName);
        };
    }

    private Object getCommonFintResource(DataFetchingEnvironment environment, FintObject fintObject) {
        CorePrincipal corePrincipal = environment.getGraphQlContext().get(CorePrincipal.class);
        List<Object> list = endpointService.getEndpoints(fintObject.getPackageName()).stream()
                .filter(endpoint -> hasAccess(endpoint, corePrincipal))
                .map(endpoint -> requestService.getCommonResource(
                        endpoint,
                        environment.getGraphQlContext().get(AUTHORIZATION),
                        corePrincipal.getUsername())
                ).toList();

        // Request to all the endpoints and collect the responses in a list

        // Merge all the data

        // return the data
        return null;
    }

    private boolean hasAccess(String string, CorePrincipal corePrincipal) {
        String[] split = string.split("/");
        return corePrincipal.getRoles().contains(String.format("FINT_Client_%s_%s", split[1], split[2]));
    }

    private Object getFintResource(DataFetchingEnvironment environment, FintObject fintObject) {
        CorePrincipal corePrincipal = environment.getGraphQlContext().get(CorePrincipal.class);
        return requestService.getResource(
                createRequestUri(environment, fintObject),
                environment.getGraphQlContext().get(AUTHORIZATION),
                corePrincipal.getUsername()
        );
    }

    private Object getFintRelationResource(DataFetchingEnvironment environment, String fieldName) {
        return getFintRelationResources(environment, fieldName).getFirst();
    }

    private List<Object> getFintRelationResources(DataFetchingEnvironment environment, String fieldName) {
        CorePrincipal corePrincipal = environment.getGraphQlContext().get(CorePrincipal.class);
        return getRelationRequestUri(environment, fieldName).stream()
                .map(link -> requestService.getResource(
                        link,
                        environment.getGraphQlContext().get(AUTHORIZATION),
                        corePrincipal.getUsername())
                ).toList();
    }

    private List<String> getRelationRequestUri(DataFetchingEnvironment environment, String fieldName) {
        Map<String, Map<String, List<Map<String, String>>>> source = environment.getSource();
        Map<String, List<Map<String, String>>> linksMap = source.get(LINKS);

        if (linksMap == null || !linksMap.containsKey(fieldName) || linksMap.get(fieldName).isEmpty()) {
            throw new MissingLinkException(fieldName);
        }

        return linksMap.get(fieldName).stream()
                .map(map -> map.get(HREF).replace(restClientConfig.getBaseUrl(), ""))
                .toList();
    }

    private String createRequestUri(DataFetchingEnvironment environment, FintObject fintObject) {
        Map.Entry<String, Object> firstArgument = contextService.getFirstArgument(environment);
        return String.format("%s/%s/%s", fintObject.getResourceUrl(), firstArgument.getKey(), firstArgument.getValue());
    }

}
