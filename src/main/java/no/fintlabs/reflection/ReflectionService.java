package no.fintlabs.reflection;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.reflection.model.FintMainObject;
import no.fintlabs.reflection.model.FintObject;
import org.reflections.Reflections;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Service
public class ReflectionService {

    private final Map<String, FintMainObject> fintMainObjects;
    private final Map<String, FintObject> fintObjects;

    public ReflectionService() {
        Reflections reflections = new Reflections("no.fint.model");
        fintMainObjects = createFintMainObjects(reflections);
        fintObjects = createFintObjects(reflections);
//        fintMainObjects.forEach((k, v) -> log.info("{}", v));
    }

    public FintObject findFintObject(String name) {
        if (fintMainObjects.containsKey(name)) {
            return fintMainObjects.get(name);
        } else if (fintObjects.containsKey(name)) {
            return fintObjects.get(name);
        } else {
            throw new IllegalArgumentException("Unknown fint object: " + name);
        }
    }

    private Map<String, FintMainObject> createFintMainObjects(Reflections reflections) {
        return reflections
                .getSubTypesOf(no.fint.model.FintMainObject.class)
                .stream()
                .collect(Collectors.toMap(
                        Class::getName,
                        FintMainObject::new));
    }

    private Map<String, FintObject> createFintObjects(Reflections reflections) {
        return reflections
                .getSubTypesOf(no.fint.model.FintObject.class)
                .stream()
                .filter(clazz -> !fintMainObjects.containsKey(clazz.getName()))
                .collect(Collectors.toMap(
                        Class::getName,
                        FintObject::new));
    }

}
