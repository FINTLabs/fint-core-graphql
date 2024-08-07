package no.fintlabs.service.datafetcher;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.config.WebClientConfig;
import no.fintlabs.core.resource.server.security.authentication.CorePrincipal;
import no.fintlabs.exception.exceptions.EntityNotFoundException;
import no.fintlabs.exception.exceptions.MissingLinkException;
import no.fintlabs.reflection.model.FintObject;
import no.fintlabs.service.EndpointService;
import no.fintlabs.service.RequestService;
import no.fintlabs.service.ResourceAssembler;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceFetcher {

    private final static String LINKS = "_links";
    private final static String HREF = "href";

    private final ContextService contextService;
    private final EndpointService endpointService;
    private final RequestService requestService;
    private final ResourceAssembler resourceAssembler;
    private final WebClientConfig webClientConfig;
    private final Cache<String, Object> errorCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    public CompletableFuture<Object> getFintResource(DataFetchingEnvironment environment, FintObject fintObject) {
        return requestService.getResource(
                buildResourceUri(environment, fintObject),
                environment
        );
    }

    public Object getCommonFintResource(DataFetchingEnvironment environment, FintObject fintObject) {
        CorePrincipal corePrincipal = environment.getGraphQlContext().get(CorePrincipal.class);
        Map.Entry<String, Object> firstArgument = contextService.getFirstArgument(environment);

        List<Object> resources = endpointService.getEndpoints(fintObject.getPackageName()).stream()
                .filter(endpoint -> hasAccess(endpoint, corePrincipal))
                .map(endpoint -> buildResourceUri(endpoint, firstArgument))
                .map(uri -> requestService.getCommonResource(uri, environment))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (resources.isEmpty()) {
            throw new EntityNotFoundException();
        } else {
            return resourceAssembler.mergeLinks(resources);
        }
    }

    public Object getFintRelationResource(DataFetchingEnvironment environment, String fieldName) {
        return getFintRelationResources(environment, fieldName).getFirst();
    }

    public List<Object> getFintRelationResources(DataFetchingEnvironment environment, String fieldName) {
        return getRelationRequestUri(environment, fieldName).stream()
                .map(link -> requestService.getRelationResource(link, environment))
                .filter(Objects::nonNull)
                .toList();
    }

    private List<String> getRelationRequestUri(DataFetchingEnvironment environment, String fieldName) {
        Map<String, Map<String, List<Map<String, String>>>> source = environment.getSource();
        Map<String, List<Map<String, String>>> linksMap = source.get(LINKS);

        if (linksMap == null || !linksMap.containsKey(fieldName) || linksMap.get(fieldName).isEmpty()) {
            if (errorCache.getIfPresent(environment.getExecutionId().toString()) == null) {
                log.error("fieldName: {}, source: {}", fieldName, source);
                errorCache.put(environment.getExecutionId().toString(), 1);
            }
            throw new MissingLinkException(fieldName);
        }

        return linksMap.get(fieldName).stream()
                .map(map -> map.get(HREF).replace(webClientConfig.getBaseUrl(), ""))
                .toList();
    }

    private boolean hasAccess(String endpoint, CorePrincipal corePrincipal) {
        String[] split = endpoint.split("/");
        return corePrincipal.getRoles().contains(String.format("FINT_Client_%s_%s", split[1], split[2]));
    }

    private String buildResourceUri(String endpoint, Map.Entry<String, Object> firstArgument) {
        return String.format("%s/%s/%s", endpoint, firstArgument.getKey(), firstArgument.getValue());
    }

    private String buildResourceUri(DataFetchingEnvironment environment, FintObject fintObject) {
        Map.Entry<String, Object> firstArgument = contextService.getFirstArgument(environment);
        return String.format("%s/%s/%s", fintObject.getResourceUrl(), firstArgument.getKey(), firstArgument.getValue());
    }

}
