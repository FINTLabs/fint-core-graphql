package no.fintlabs.reflection;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.reflection.model.FintMainObject;
import org.reflections.Reflections;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Service
public class ReflectionService {

    private final Map<String, FintMainObject> fintMainObjects;

    public ReflectionService() {
        Reflections reflections = new Reflections("no.fint.model");
        fintMainObjects = createFintMainObjects(reflections);
        fintMainObjects.forEach((k, v) -> log.info("{}", v));
    }

    private Map<String, FintMainObject> createFintMainObjects(Reflections reflections) {
        return reflections
                .getSubTypesOf(no.fint.model.FintMainObject.class)
                .stream()
                .collect(Collectors.toMap(
                        Class::getName,
                        FintMainObject::new));
    }

}
