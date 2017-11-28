COPYRIGHT &copy; 2017 AUTODESK INC.

# Example of more complex JSON schema [Plug-in default]

This example introduces:  
- Two levels of grouping: **group** / **test**,  
- Each type definitions are gathered inside an array.  
- Parameters or group of parameters possible at every level,  
- Thresholds limited HERE to the result section,  

## Example of result file to parse

```json
{
    "groups":[
        {
            "name":"group 1",
            "description":"group nb 1",
            "tests":[
                {
                    "name":"test 1",
                    "description":"test nb 1",
                    "parameters":[
                        {
                            "name":"parameter 1",
                            "description":"parameter nb 1",
                            "unit": "m",
                            "value": "bouh"
                        },
                        {
                            "name":"parameter 1",
                            "description":"parameter nb 1",
                            "unit": "m",
                            "value": true
                        }
                    ],
                    "results":[
                        {
                            "name":"result 1",
                            "description":"result nb 1",
                            "unit": "m",
                            "boolValue": false
                        },
                        {
                            "name":"result 2",
                            "description":"result nb 2",
                            "unit": "m",
                            "dblValue": 15.46,
                            "thresholds":[
                                {
                                    "method":"absolute",
                                    "minimum": 10,
                                    "maximum": 17
                                }
                            ]
                        }
                    ]
                }
            ]
        }
    ]
}


```

## Associated schema

```json
{
    "description": "Default schema of the Benchmark Jenkins plug-in",
    "failure": { "value": true },
    "type": "object",
    "properties":{
        "groups": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "name": { "type": "name" },
                    "description": { "type": "Description"},
                    "parameters":{
                        "type": "array",
                        "items": {
                            "type": "parameter",
                            "properties": {
                                "name": { "type": "name" },
                                "description": { "type": "description" },
                                "unit": { "type": "unit" },
                                "value": { "type": "value" }
                            }
                        }
                    },
                    "tests": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "name": { "type": "name" },
                                "description": { "type": "description" },
                                "parameters":{
                                    "type": "array",
                                    "items": {
                                        "type": "parameter",
                                        "properties": {
                                            "name": { "type": "name" },
                                            "description": { "type": "description" },
                                            "unit": { "type": "unit" },
                                            "value": { "type": "value" }
                                        }
                                    }
                                },
                                "results": {
                                    "type": "array",
                                    "items":{
                                        "type": "result",
                                        "properties": {
                                            "name": {"type": "name"},
                                            "description": {"type": "description"},
                                            "unit": {"type": "unit"},
                                            "boolValue": {"type": "boolean"},
                                            "intValue": {"type": "integer"},
                                            "dblValue": {"type": "double"},
                                            "value": {"type": "value"},
                                            "thresholds": {
                                                "type": "array",
                                                "items": {
                                                    "type": "threshold",
                                                    "properties": {
                                                        "method": { "type": "method" },
                                                        "minimum": {"type": "minimum"},
                                                        "maximum": {"type": "maximum"},
                                                        "delta": {"type": "delta"},
                                                        "percentage": {"type": "percentage"}
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
```
