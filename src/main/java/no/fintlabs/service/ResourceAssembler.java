package no.fintlabs.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResourceAssembler {

    public Object mergeContent(List<Object> resources) {
        LinkedHashMap<String, Object> firstResource = castToLinkedHashMap(resources.get(0));
        LinkedHashMap<String, List<Map<String, String>>> consolidatedLinks = getLinks(firstResource);

        resources.stream().skip(1)
                .map(this::castToLinkedHashMap)
                .map(this::getLinks)
                .forEach(links -> links.forEach((key, value) ->
                        consolidatedLinks.merge(key, value, (existingVal, newVal) -> {
                            existingVal.addAll(newVal);
                            return existingVal;
                        })));

        firstResource.put("_links", consolidatedLinks);
        return firstResource;
    }

    @SuppressWarnings("unchecked")
    private LinkedHashMap<String, Object> castToLinkedHashMap(Object resource) {
        return (LinkedHashMap<String, Object>) resource;
    }

    @SuppressWarnings("unchecked")
    private LinkedHashMap<String, List<Map<String, String>>> getLinks(LinkedHashMap<String, Object> resource) {
        return (LinkedHashMap<String, List<Map<String, String>>>) resource.get("_links");
    }

}
