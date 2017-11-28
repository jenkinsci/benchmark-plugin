COPYRIGHT &copy; 2017 AUTODESK INC.

# Example of Simple XML schema

This example introduces:  
- One result as the root of the result file,  
- Parameters & thresholds inside the result section.  

## Example of result file to parse

```xml  
<?xml version="1.0" encoding="UTF-8"?>  
<!-- Test file for simplest schema -->  
<result>  
    <name>result_1</name>  
    <description>description_result_1</description>  
    <value>13.5</value>  
    <unit>meters</unit>  
    <parameter>  
        <name>parameter_1</name>  
        <description>description_parameter_1</description>  
        <value>10</value>  
        <unit>minutes</unit>  
    </parameter>  
    <threshold>  
        <method>absolute</method>  
        <minimum>13</minimum>  
        <maximum>15</maximum>  
    </threshold>  
</result>  
```

## Associated schema

```xml  
<?xml version="1.0"?>  
<!-- Simple result schema -->  
  
<xs:schema xmlns="http://autodesk.com"
           xmlns:jbs="http://autodesk.com/jenkins/jbs"
           xmlns:xs="http://www.w3.org/2001/XMLSchema" >

    <jbs:failure type="jbs:boolean">true</jbs:failure>

    <xs:complexType name="Parameter" type="jbs:parameter">
        <xs:sequence>  
            <xs:element name="name" type="jbs:name"/>
            <xs:element name="description" type="jbs:description"/>
            <xs:element name="value" type="jbs:value"/>
            <xs:element name="unit" type="jbs:unit"/>
        </xs:sequence>
    </xs:complexType>
  
    <xs:complexType name="Threshold" type="jbs:threshold">
        <xs:sequence>
            <xs:element name="method" type="jbs:method"/>
            <xs:element name="minimum" type="jbs:minimum"/>
            <xs:element name="maximum" type="jbs:maximum"/>
            <xs:element name="delta" type="jbs:delta"/>
            <xs:element name="percentage" type="jbs:percentage"/>
        </xs:sequence>
    </xs:complexType>
  
    <xs:element name="result" type="jbs:result">
        <xs:complexType>
            <xs:sequence>
                <xs:element name="name" type="jbs:name"/>
                <xs:element name="description" type="jbs:description"/>
                <xs:element name="value" type="jbs:value"/>
                <xs:element name="unit" type="jbs:unit"/>
                <xs:element name="id" type="jbs:id"/>
                <xs:element name="message" type="jbs:message"/>
                <xs:element name="parameter" type="Parameter" minOccurs="0" maxOccurs="unbounded"/>
                <xs:element name="threshold" type="Threshold" minOccurs="0" maxOccurs="unbounded"/>
            </xs:sequence>
        </xs:complexType>
    </xs:element>
</xs:schema>
```