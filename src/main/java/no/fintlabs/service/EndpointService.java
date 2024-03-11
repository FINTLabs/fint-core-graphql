package no.fintlabs.service;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.reflection.ReflectionService;
import no.fintlabs.reflection.model.FintObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class EndpointService {

    private final ReflectionService reflectionService;
    private final Map<String, Set<String>> commonEndpoints = new HashMap<>();

    public EndpointService(ReflectionService reflectionService, @Qualifier("fintObjects") Map<String, FintObject> fintObjects) {
        this.reflectionService = reflectionService;
        addEndpoints(fintObjects);
    }

    private void addEndpoints(Map<String, FintObject> fintObjects) {
        fintObjects.values().forEach(fintObject -> {
            if (fintObject.isMainObject() && !fintObject.isCommon()) {
                fintObject.getRelations().forEach(fintRelation -> {
                    FintObject relationObject = reflectionService.getFintObject(fintRelation.packageName());
                    if (relationObject.isCommon()) {
                        addCommonEndpoint(fintRelation.packageName(), String.format("%s/%s", fintObject.getComponentUri(), relationObject.getSimpleName()));
                    }
                });
            }
        });
    }

    public void addCommonEndpoint(String fintObjectName, String endpoint) {
        commonEndpoints.computeIfAbsent(fintObjectName, k -> new HashSet<>()).add(endpoint);
    }

    public Set<String> getEndpoints(String fintObjectName) {
        return commonEndpoints.get(fintObjectName);
    }

}
