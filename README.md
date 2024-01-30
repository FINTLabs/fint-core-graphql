# Fint core GraphQL

## Project overview
This project is designed as an improvement upon fint-graphql, with a specific focus on enhancing the way GraphQL schemas are created. 
By employing Java reflection to dynamically generate schemas from Information model objects.

## Usage
The GraphQL Schema follows the structure of FINT Informationmodel.
We do not support the first query to fetch multiple resources, so you need to specify which resource you want to get by using their Identifikator argument.
```graphql
query getElevGjest {
  Elev(elevnummer: "123") {
    gjest
  }
}
```
Please take note that only the first argument will get used, so any other argument coming after will be ignored.

Even though you can only specify one element at the first query, doesn't mean you can only get one reference at a time.
```graphql
query getSkoler {
	Skole(systemId: "1579") {
		basisgruppe {
      navn
    }
	}
}
```
This would get all the basisgrupper for that resource, which could be a lot depending on the resource.
