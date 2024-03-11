package no.fintlabs.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResourceAssemblerTest {

    private ResourceAssembler resourceAssembler;

    @BeforeEach
    void setUp() {
        resourceAssembler = new ResourceAssembler();
    }

    @SuppressWarnings("unchecked")
    @Test
    void mergeLinks_shouldMergeAllLinksIntoFirstResource() {
        LinkedHashMap<String, Object> resource1 = new LinkedHashMap<>();
        LinkedHashMap<String, List<Map<String, String>>> links1 = new LinkedHashMap<>();
        links1.put("self", Arrays.asList(Map.of("href", "http://example.com/1")));
        resource1.put("_links", links1);

        LinkedHashMap<String, Object> resource2 = new LinkedHashMap<>();
        LinkedHashMap<String, List<Map<String, String>>> links2 = new LinkedHashMap<>();
        links2.put("related", Arrays.asList(Map.of("href", "http://example.com/2")));
        resource2.put("_links", links2);

        List<Object> resources = Arrays.asList(resource1, resource2);

        Object mergedResource = resourceAssembler.mergeLinks(resources);

        assertTrue(mergedResource instanceof LinkedHashMap);

        LinkedHashMap<String, Object> mergedLinks = (LinkedHashMap<String, Object>) mergedResource;
        assertTrue(mergedLinks.containsKey("_links"));

        LinkedHashMap<String, List<Map<String, String>>> links = (LinkedHashMap<String, List<Map<String, String>>>) mergedLinks.get("_links");
        assertTrue(links.containsKey("self"));
        assertTrue(links.containsKey("related"));
        assertEquals(1, links.get("self").size());
        assertEquals("http://example.com/1", links.get("self").get(0).get("href"));
        assertEquals(1, links.get("related").size());
        assertEquals("http://example.com/2", links.get("related").get(0).get("href"));
    }
}
