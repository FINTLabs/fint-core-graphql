# Fint core GraphQL

## Project overview
This project is designed as an improvement upon fint-graphql, with a specific focus on enhancing the way GraphQL schemas are created. 
By employing Java reflection to dynamically generate schemas from Information model objects.

## Reflection
The main functionality of this project is using Java reflection.
ReflectionService looks into any Java classes that inherits the FintObject interface at 'no.fint.model'
For every FintObject found we create a FintObject.

### FintObject
Not to be confused with the FintObject interface in the fint model libraries, this object contains the information we need to create a GraphQLType for every object.
#### Fields:
* boolean isMainObject: if the original object is assignable from FintMainObject (The objects that should be queryable)
* String name: unique name for the FintObject
* String packageName: full path to original object (is used as key to get FintObjects)
* String domainName: domain name is the first package after no.fint.model (no.fint.model.utdanning.vurdering.Fravar = utdanning)
* List<Field> fields: contains all Java fields
* List<FintRelation> relations: contains all relations
* Set<String> identificatorFields: contains all identificator field names


### FintRelation
Some objects may have a relation to other objects.
For example a Student in FINT has a relation to Person.
#### Fields:
* String relationName: the name of the relation (this may not be equal to the actual type used)
* String packageName: the full path to the Fint object it relates to
* String multiplicity: information about the multiplicity (0..1 is 0 to 1) [Not in use at the moment]**
