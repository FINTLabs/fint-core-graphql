# Fint core GraphQL

## Project overview

This project is designed as an improvement upon fint-graphql, with a specific focus on enhancing the way GraphQL schemas
are created.
By employing Java reflection to dynamically generate schemas from Information model objects.

## Usage

The GraphQL Schema follows the structure of FINT Informationmodel.
We do not support the first query to fetch multiple resources, so you need to specify which resource you want to get by
using their Identifikator argument.

```graphql
query getElevGjest {
  Elev(elevnummer: "123") {
    gjest
  }
}
```

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

### Exceptions

We have implemented a more descriptive and detailed exception handler that will give the user a more detailed
description, and custom exception thrown based on what goes wrong.

```json
{
  "errors": [
    {
      "message": "Missing required argument",
      "locations": [
        {
          "line": 2,
          "column": 2
        }
      ],
      "path": [
        "Skole"
      ],
      "extensions": {
        "classification": "MissingArgumentException"
      }
    }
  ],
  "data": {
    "Skole": null
  }
}
```

The custom exception will be shown in the classification value. Any unexpected errors might look more cryptic or chaotic, so please let us know if you encounter any weird problems.


### Arguments

When specifying a resource, only the first argument will be used.
Any other arguments coming after will get ignored.