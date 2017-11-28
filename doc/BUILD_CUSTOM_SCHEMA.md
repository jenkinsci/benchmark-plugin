COPYRIGHT &copy; 2017 AUTODESK INC.

# How to build a custom schema

The 'Benchmark Plug-in' is designed to load any JSON or XML result file format.  

JSON schema examples:  
- [Simple JSON schema](./EXAMPLE_SCHEMA_JSON_SIMPLE.md)  
- [Standard JSON schema](./EXAMPLE_SCHEMA_JSON_DEFAULT.md)  

XML schema examples:  
- [Simple XML schema](./EXAMPLE_SCHEMA_XML_SIMPLE.md)  
- [Standard XML schema](./EXAMPLE_SCHEMA_XML_DEFAULT.md)  

## General rules  

The plug-in uses a schema to map the test result file content to the plug-in components. The appropriate default schema is selected from the result file extension. At the moment, this plug-in only supports JSON and XML formats. The default schema for both formats can be found [HERE](src/main/resources/schemas) and are reproduced below as illustrations.  

To accommodate a wide choice of schema, the configuration page has an advanced field to input a custom schema.

Below is the list of identifiers that the plug-in will recognize in a custom schema:  

- Standard tree blocks e.g. group/subgroup/test:

| Identifier           | Description                           | Format   |  
|----------------------|---------------------------------------|----------|  
| name                 | Element name                          | String   |  
| description          | Element description                   | String   |  

**Note:** Standard block types are dependent on the file format e.g. **array** or **object** for JSON and **xs:complexType** or **xs:sequence** for XML

- Result:

| Identifier           | Description                           | Format   |  
|----------------------|---------------------------------------|----------|  
| result               | Block type                            |          |  
| name                 | Element name                          | String   |  
| description          | Element description                   | String   |  
| unit                 | Result unit                           | String   |  
| value                | Automatically detected format         | Variable |  
| boolean              | Boolean value                         | Boolean  |  
| booleankey           | Key is Boolean value                  | Boolean  |  
| integer              | Integer value                         | Integer  |  
| double               | Double value                          | Double   |  
| id                   | ID/Code associated to result          | Integer  |  
| message              | Message associated to result          | String   |  


- Parameter:

| Identifier           | Description                            | Format   |  
|----------------------|----------------------------------------|----------|  
| parameter            | Block type                             |          |  
| name                 | Element name                           | String   |  
| description          | Element description                    | String   |  
| value                | Format auto-detected                   |          |  
| unit                 | Result unit                            | String   |  

- Threshold:

| Identifier           | Description                            | Format   |  
|----------------------|----------------------------------------|----------|  
| threshold            | Block type                             |          |  
| method               | Threshold method (see list below)      | String   |  
| minimum              | Minimum for absolute threshold         | Double   |  
| maximum              | Maximum for absolute threshold         | Double   |  
| delta                | Delta threshold                        | Double   |  
| percentage           | Percentage threshold                   | Double   |  

Parameter and Threshold blocks are assumed to be one layer deep. No other component may be added inside Parameter or Threshold blocks. However, parameter and thresholds may be grouped.

Another necessary component is how a **Boolean test** is detected as a failure (see below for format specific).

The key to custom define a boolean failure is: **"failure"** and should be placed at the beginning of the schema. The value can be a Boolean(false) or a an integer(1) or even a string ("failure"). The default is the string: **"failure"** and is case-less meaning "Failure" or "FAILURE" are also valid. See format specifics for declarations.  

**SCHEMA GENERAL RULES**
- Properties are used to define test environment. Properties can be placed at any levels and will be associated to all content from that level.  
- Thresholds are used to define result thresholds and might trigger build failure. Thresholds can be placed at any levels and will be associated to all content from that level.  
- The general keys/tags **Group**/**Test**/**Result**/**Threshold**/**Parameter** are used to separate content but can be bypassed if the content are added as 1 to 1 (one result with one threshold).

### Enumerations of Threshold Methods
Current list (ref:[here](src/main/java/org/jenkinsci/plugins/benchmark/thresholds/Threshold.java)):  
- **Absolute** - Defined by [minimum, maximum],  
- **Percentage** - Percentage between last build and current,  
- **PercentageAverage** - Percentage between average and current,  
- **Delta** - Delta between last build and current,  
- **DeltaAverage** - Delta between average and current.  

## JSON specifics

The default JSON schema as well as other examples of schema(s) are located [HERE](src/main/resource/schemas).  

**JSON SCHEMA RULES:**  
- Identifier case is irrelevant,  
- An element can be a **object**/**array**/**result**/**threshold**/**parameter**,  
- In addition to **result** and **parameter**, the system offers **resultFull** and **parameterFull** where the content is not an object but a simple key/value,  
- All key associated to a plug-in component require a **"type"** sub-key.  
- Use the following sequence to move deeper into the JSON content.  
```json
    "####":{
        "type": "object",
        "properties": {
            "..."
        }
    }
```
or in array form:
```json
    "####":{
        "type": "array",
        "items": {
            "type": "object",
            "properties":{
                "..."
            }
        }
    }
```
- The declaration of how **Boolean test failure** is identified, is placed at the beginning of the schema with:  
```json
    "failure": {
        "value": true
    }
```
or
```json
    "failure": {
        "value": "####"
    }
```
or
```json
    "failure": {
        "value": 1
    }
```
or
```json
    "failure": {
        "value": 1.5,
        "compare": "belowOrEqual
    }
```
or
```json
    "failure": {
        "value": true
    }
```
or
```json
    "failure": {
        "type":"key",
        "value": "####"
    }
```
**WARNING:** Failure must be defined inside the schema even for booleans.  
**Note:** Available compare modes:  
- **belowOrEqual**,  
- **aboveOrEqual**,  
- **above**,  
- **below**,  
- **equal**.  
**Note:** To declare multiple failure modes, create an array called **"failures"**.
**Note:** Any additional content can be added to enrich the schema but will not affect plug-in operations.  

## XML specifics

The default XML schema as well as other examples of schemas are located [HERE](src/main/resource/schemas).  

Standard XML file skeleton:
```xml
<?xml version="1.0"?>  
<xs:schema xmlns="http://autodesk.com"  
           xmlns:jbs="http://autodesk.com/jenkins/jbs"  
           xmlns:xs="http://www.w3.org/2001/XMLSchema" >  
...  
</xs:schema>  
```

**XML SCHEMA RULES:**  
- The above identifiers for **XML** must include the namespace: **jbs** such as **jbs:name**.  
- The following are valid schema tags:  
  - **xs:element**  
  - **xs:attribute**  
  - **xs:complexType**  
  - **xs:sequence**  
- As plug-in component, the tags **xs:element** / **xs:attribute** / **xs:complexType** require a **type** attribute such as **jbs:parameter**.  
- Use the following sequence to move deeper into the XML content:  
```xml
    <jbs:element name="####">  
        <jbs:complexType>  
            <jbs:sequence>  
                ...  
            </jbs:sequence>  
        </jbs:complexType>  
    </jbs:element>  
```
or in array form:  
```xml
    <jbs:element name="####" minOccurs="0" maxOccurs="unbounded">  
        <jbs:complexType>  
            <jbs:sequence>  
                ...
            </jbs:sequence>  
        </jbs:complexType>  
    </jbs:element>  
```
- The declaration of how **Boolean test failure** is identified, is placed at the beginning of the schema with:
```xml  
<jbs:failure type="jbs:boolean">true</jbs:failure>
```  
or  
```xml  
<jbs:failure type="jbs:string">#####</jbs:failure>
```  
or  
```xml  
<jbs:failure type="jbs:element">#####</jbs:failure>
```  
or  
```xml  
<jbs:failure type="jbs:value" compare="jbs:aboveOrEqual">####</jbs:failure>
```  

**WARNING:** Failure must be defined inside the schema even for booleans.  
**Note:** Available compare modes:  
- **belowOrEqual**,  
- **aboveOrEqual**,  
- **above**,  
- **below**,  
- **equal**.  

**Note:** There can be multiple such declaration. Each will be checked.  

