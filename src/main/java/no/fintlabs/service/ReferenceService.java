package no.fintlabs.service;

import graphql.schema.GraphQLObjectType;
import no.fintlabs.reflection.model.FintObject;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ReferenceService {

    private final Map<Integer, FintObject> fintObjectReferences = new HashMap<>();
    private final Map<String, GraphQLObjectType> relationFintObjectReference = new HashMap<>();

    public void addReferenecs(FintObject fintObject, GraphQLObjectType objectType) {
        fintObjectReferences.put(objectType.hashCode(), fintObject);
        relationFintObjectReference.put(fintObject.getName(), objectType);
    }

    public GraphQLObjectType getRelationFintObject(String name) {
        return relationFintObjectReference.get(name);
    }

    public FintObject getFintObject(int hashCode) {
        return fintObjectReferences.get(hashCode);
    }

}
