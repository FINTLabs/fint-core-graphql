package no.fintlabs.reflection.model;

import no.fint.model.felles.kompleksedatatyper.Identifikator;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class FintMainObject extends FintObject {

    private Set<String> identificatorFields;

    public FintMainObject(Class<?> clazz) {
        super(clazz);
        identificatorFields = setIdentificatorFields();
    }

    private Set<String> setIdentificatorFields() {
        Set<String> identifikatorFields = new HashSet<>();
        for (Field field : super.getFields()) {
            if (field.getType().equals(Identifikator.class)) {
                identifikatorFields.add(field.getName());
            }
        }
        return identifikatorFields;
    }

}
