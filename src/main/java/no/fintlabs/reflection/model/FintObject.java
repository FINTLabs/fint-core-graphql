package no.fintlabs.reflection.model;

import lombok.Data;
import no.fint.model.FintMainObject;
import no.fint.model.FintReference;
import no.fint.model.felles.kompleksedatatyper.Identifikator;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

@Data
public class FintObject {

    private final boolean isMainObject;
    private final boolean isAbstract;
    private final boolean isCommon;
    private final boolean isReference;
    private final String name;
    private final String simpleName;
    private final String packageName;
    private final String componentUri;
    private final String domainName;
    private final String resourceUrl;
    private final List<Field> fields;
    private final List<FintRelation> relations;
    private final Set<String> identificatorFields;

    public FintObject(Class<?> clazz, boolean hasUniqueName) {
        packageName = clazz.getName();
        domainName = setDomainName();
        isCommon = domainName.equalsIgnoreCase("felles");
        name = setName(clazz, hasUniqueName);
        fields = getAllFields(clazz);
        componentUri = setComponentUri(clazz);
        resourceUrl = setResourceUrl(clazz);
        relations = getAllRelations(clazz);
        isMainObject = setIsMainObject(clazz);
        identificatorFields = setIdentificatorFields();
        isAbstract = Modifier.isAbstract(clazz.getModifiers());
        isReference = FintReference.class.isAssignableFrom(clazz);
        simpleName = clazz.getSimpleName().toLowerCase();
    }

    private String setComponentUri(Class<?> clazz) {
        String[] parts = clazz.getName().split("\\.");
        return "/" + String.join("/", Arrays.copyOfRange(parts, 3, 5));
    }

    private String setResourceUrl(Class<?> clazz) {
        return String.format("%s/%s", componentUri, clazz.getSimpleName().toLowerCase());
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
                        String multiplicityLabel = (String) getMultiplicityMethod.invoke(enumConstant);
                        Multiplicity multiplicity = Multiplicity.fromLabel(multiplicityLabel);
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
