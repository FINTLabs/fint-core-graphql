package no.fintlabs.reflection.model;

import lombok.Data;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
public class FintObject {

    private final String name;
    private final String packageName;
    private final List<Field> fields;

    public FintObject(Class<?> clazz) {
        this.name = clazz.getSimpleName();
        this.packageName = clazz.getPackage().getName();
        this.fields = getAllFields(clazz);
    }

    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

}
