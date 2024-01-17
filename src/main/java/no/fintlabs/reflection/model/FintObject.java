package no.fintlabs.reflection.model;

import lombok.Data;
import no.fint.model.FintMainObject;
import no.fint.model.felles.kompleksedatatyper.Identifikator;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

@Data
public class FintObject {

    private final boolean isMainObject;
    private final String name;
    private final String packageName;
    private final String domainName;
    private final String componentUrl;
    private final String resourceUrl;
    private final List<Field> fields;
    private final List<FintRelation> relations;
    private final Set<String> identificatorFields;

    public FintObject(Class<?> clazz, boolean hasUniqueName) {
        this.packageName = clazz.getName();
        this.domainName = setDomainName();
        this.name = setName(clazz, hasUniqueName);
        this.fields = getAllFields(clazz);
        this.componentUrl = setComponentUrl(clazz);
        this.resourceUrl = setResourceUrl(clazz);
        this.relations = getAllRelations(clazz);
        this.isMainObject = setIsMainObject(clazz);
        this.identificatorFields = setIdentificatorFields();
    }

    private String setResourceUrl(Class<?> clazz) {
        return "/" + clazz.getSimpleName().toLowerCase();
    }

    private String setComponentUrl(Class<?> clazz) {
        String[] parts = clazz.getName().split("\\.");
        return "/" + String.join("/", Arrays.copyOfRange(parts, 3, 5));
    }

    private boolean setIsMainObject(Class<?> clazz) {
        return FintMainObject.class.isAssignableFrom(clazz);
    }

    public boolean shouldBeProcessed() {
        return !fields.isEmpty() || !relations.isEmpty();
    }

    private String setName(Class<?> clazz, boolean hasUniqueName) {
        if (hasUniqueName) {
            return clazz.getSimpleName();
        } else {
            return domainName + clazz.getSimpleName();
        }
    }

    private String setDomainName() {
        String[] split = packageName.split("\\.");
        return split[3].substring(0, 1).toUpperCase() + split[3].substring(1);
    }

    // TODO: CT-1136: Add interface for relations
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

    private Set<String> setIdentificatorFields() {
        Set<String> identifikatorFields = new HashSet<>();
        for (Field field : getFields()) {
            if (field.getType().equals(Identifikator.class)) {
                identifikatorFields.add(field.getName());
            }
        }
        return identifikatorFields;
    }

}
