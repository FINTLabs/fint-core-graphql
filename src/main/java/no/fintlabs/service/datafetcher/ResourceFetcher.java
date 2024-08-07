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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

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

    public Mono<Object> getCommonFintResources(DataFetchingEnvironment environment, FintObject fintObject) {
        CorePrincipal corePrincipal = environment.getGraphQlContext().get(CorePrincipal.class);
        Map.Entry<String, Object> firstArgument = contextService.getFirstArgument(environment);

        Stream<Mono<Object>> monoStream = endpointService.getEndpoints(fintObject.getPackageName()).stream()
                .filter(endpoint -> hasAccess(endpoint, corePrincipal))
                .map(endpoint -> buildResourceUri(endpoint, firstArgument))
                .map(uri -> requestService.getCommonResource(uri, environment));

        Flux<Object> resourceFlux = Flux.fromStream(monoStream)
                .flatMap(mono -> mono.onErrorResume(e -> Mono.empty()))
                .filter(Objects::nonNull);

        return resourceFlux.collectList()
                .flatMap(resources -> {
                    if (resources.isEmpty()) {
                        return Mono.error(new EntityNotFoundException());
                    } else {
                        return resourceAssembler.mergeLinks(resources);
                    }
                });
    }


    public Mono<Object> getFintRelationResource(DataFetchingEnvironment environment, String fieldName) {
        return getFintRelationResources(environment, fieldName).next();
    }

    public Flux<Object> getFintRelationResources(DataFetchingEnvironment environment, String fieldName) {
        return Flux.fromIterable(getRelationRequestUri(environment, fieldName))
                .flatMap(link -> requestService.getRelationResource(link, environment)
                        .onErrorResume(e -> Mono.empty()))
                .filter(Objects::nonNull);
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
