package no.fintlabs;

import graphql.schema.GraphQLObjectType;
import no.fintlabs.reflection.model.FintObject;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ReferenceService {

    private final Map<Integer, FintObject> fintObjectReferences = new HashMap<>();
    private final Map<Integer, GraphQLObjectType> objectTypeReferences = new HashMap<>();

    public void addReferenecs(FintObject fintObject, GraphQLObjectType objectType) {
        fintObjectReferences.put(objectType.hashCode(), fintObject);
        objectTypeReferences.put(fintObject.hashCode(), objectType);
    }

    public boolean containsFintObject(int hashCode) {
        return fintObjectReferences.containsKey(hashCode);
    }

    public FintObject getFintObject(int hashCode) {
        return fintObjectReferences.get(hashCode);
    }

    public GraphQLObjectType getObjectType(int hashCode) {
        return objectTypeReferences.get(hashCode);
    }

    public void addFintObject(int hashCode, FintObject fintObject) {
        fintObjectReferences.put(hashCode, fintObject);
    }

    public void addObjectType(int hashCode, GraphQLObjectType objectType) {
        objectTypeReferences.put(hashCode, objectType);
    }

}
