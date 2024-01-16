package no.fintlabs.reflection.model;

import lombok.Data;

@Data
public class FintMainObject {

    private final String name;
    private final String packageName;

    public FintMainObject(Class<?> clazz) {
        this.name = clazz.getSimpleName();
        this.packageName = clazz.getPackage().getName();
    }

}
