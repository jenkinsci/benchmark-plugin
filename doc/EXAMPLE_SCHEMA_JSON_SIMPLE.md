COPYRIGHT &copy; 2017 AUTODESK INC.

# Example of Simple JSON schema

This example introduces:  
- One result as the root of the result file,  
- Parameters & thresholds inside the result section,  
- Parameter & threshold definitions are gathered inside an array.  

## Example of result file to parse

```json
{
    "name": "result_1",
    "description": "description_res_1",
    "value": 13.5,
    "unit": "meters",
    "parameters":[
        {
            "name": "parameter_1",
            "description": "description_par_1",
            "value": 10,
            "unit": "minutes"
        }
    ],
    "thresholds":[
        {
            "method": "absolute",
            "minimum": 13,
            "maximum": 15
        }
    ]
}
```

## Associated schema

```json
{
    "description": "Simplest result",
    "failure": { "value": true },
    "type": "result",
    "properties": {
        "name": { "type": "name" },
        "description" : { "type": "description"},
        "value": {"type": "value"},
        "unit": {"type": "unit"},
        "id": {"type": "id"},
        "message": {"type": "message"},
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
        "thresholds": {
            "type": "array",
            "items": {
                "type": "Threshold",
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
```