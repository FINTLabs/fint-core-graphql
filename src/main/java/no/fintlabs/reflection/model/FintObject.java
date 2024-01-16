package no.fintlabs.reflection.model;

import lombok.Data;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
public class FintObject {

    private final String name;
    private final String packageName;
    private final List<Field> fields;
    private final List<FintRelation> relations;

    public FintObject(Class<?> clazz) {
        this.name = clazz.getSimpleName();
        this.packageName = clazz.getName();
        this.fields = getAllFields(clazz);
        this.relations = getAllRelations(clazz);
    }

    public boolean shouldBeProcessed() {
        return !fields.isEmpty() || !relations.isEmpty();
    }

    private List<FintRelation> getAllRelations(Class<?> clazz) {
        List<FintRelation> relations = new ArrayList<>();
        Class<?>[] declaredClasses = clazz.getDeclaredClasses();

        for (Class<?> innerClass : declaredClasses) {
            if (innerClass.isEnum()) {
                Object[] enumConstants = innerClass.getEnumConstants();
                for (Object enumConstant : enumConstants) {
                    try {
                        Method getTypeNameMethod = innerClass.getMethod("getTypeName");
                        Method getMultiplicityMethod = innerClass.getMethod("getMultiplicity");

                        String typeName = (String) getTypeNameMethod.invoke(enumConstant);
                        String multiplicity = (String) getMultiplicityMethod.invoke(enumConstant);
                        relations.add(new FintRelation(enumConstant.toString(), typeName, multiplicity));
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return relations;
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
