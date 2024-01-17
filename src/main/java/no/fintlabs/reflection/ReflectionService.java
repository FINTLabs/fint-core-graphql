package no.fintlabs.reflection;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.reflection.model.FintObject;
import org.reflections.Reflections;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Service
public class ReflectionService {

    private final Map<String, FintObject> fintObjects;
    private final Map<String, Integer> nameCounts = new HashMap<>();

    public ReflectionService() {
        fintObjects = createFintObjects();
    }

    public FintObject getFintObject(String name) {
        if (fintObjects.containsKey(name)) {
            return fintObjects.get(name);
        } else {
            throw new RuntimeException("Could not find FintObject with name " + name);
        }
    }

    private Map<String, FintObject> createFintObjects() {
        Set<Class<? extends no.fint.model.FintObject>> subTypesOf = new Reflections("no.fint.model")
                .getSubTypesOf(no.fint.model.FintObject.class);
        gatherNameCounts(subTypesOf);
        return mapToFintObjects(subTypesOf);
    }

    private Map<String, FintObject> mapToFintObjects(Set<Class<? extends no.fint.model.FintObject>> subTypes) {
        return subTypes.stream()
                .collect(Collectors.toMap(
                        Class::getName,
                        clazz -> new FintObject(clazz, hasUniqueName(clazz))));
    }

    private void gatherNameCounts(Set<Class<? extends no.fint.model.FintObject>> subTypes) {
        subTypes.forEach(clazz -> {
            String simpleName = clazz.getSimpleName();
            nameCounts.put(simpleName, nameCounts.getOrDefault(simpleName, 0) + 1);
        });
    }

    private boolean hasUniqueName(Class<?> clazz) {
        return nameCounts.getOrDefault(clazz.getSimpleName(), 0) == 1;
    }

}
