COPYRIGHT &copy; 2017 AUTODESK INC.

# Registered schemas


## List

| Name       | Format      | Description                                                |  
|------------|-------------|------------------------------------------------------------|  
| simplest   | XML/JSON    | 1 level - Result only with parameters.                     |  
| default    | XML/JSON    | 3 level - Group/Test/Result with parameters at each level. |  

Note: To get details about the schemas, please access the content inside the github repository at:
``` 
    src/main/resources/schemas
```

## Add a new registered schema

- Add the related schema file(s) inside:
```
    src/main/resources/schemas
```
- Edit the static array describing the registered schema inside class BenchmarkPublisher inside:
```
    src/main/java/org.jenkinsci.plugins.benchmark.core.BenchmarkPublisher.java
```
- Finally, please, edit the list inside this README file:
```
    src/main/resources/schemas/README.md
```