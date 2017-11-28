COPYRIGHT &copy; 2017 AUTODESK INC.

# Example of more complex XML schema [Plug-in default]

This example introduces:  
- Two levels of grouping: **group** / **test**,  
- Parameters or group of parameters possible at every level,  
- Thresholds limited HERE to the result section.  

## Example of result file to parse

```xml
<?xml version="1.0"?>
<!-- Default result content -->
<group name="group 1">
    <description>This is group #1</description>
    <parameter name="parameter 1">
        <description>This is parameter #1</description>
        <value>54.8</value>
        <unit>lumen</unit>
    </parameter>
    <parameter name="parameter 2">
        <description>This is parameter #2</description>
        <value>105.8</value>
        <unit>volt</unit>
    </parameter>
    <test name="test 1">
        <description>This is test #1</description>
        <result name="result 1">
            <description>This is result #1</description>
            <dblValue>12.5</dblValue>
            <unit>meter</unit>
            <threshold method="absolute">
                <minimum>10</minimum>
                <maximum>15</maximum>
            </threshold>
        </result>
        <result name="result 2">
            <description>This is result #2</description>
            <dblValue>34.5</dblValue>
            <unit>pascal</unit>
        </result>
    </test>
</group>
```

## Associated schema

```xml
<?xml version="1.0"?>
<!-- Default result schema -->

<xs:schema xmlns="http://autodesk.com"
           xmlns:jbs="http://autodesk.com/jenkins/jbs"
           xmlns:xs="http://www.w3.org/2001/XMLSchema">

    <jbs:failure type="jbs:boolean">true</jbs:failure>

    <xs:complexType name="Threshold" type="jbs:threshold">
        <xs:sequence>
            <xs:element name="minimum" type="jbs:minimum"/>
            <xs:element name="maximum" type="jbs:maximum"/>
            <xs:element name="delta" type="jbs:delta"/>
            <xs:element name="percentage" type="jbs:percentage"/>
        </xs:sequence>
        <xs:attribute name="method" type="jbs:method"/>
    </xs:complexType>

    <xs:complexType name="Parameter" type="jbs:parameter">
        <xs:sequence>
            <xs:element name="description" type="jbs:description"/>
            <xs:element name="unit" type="jbs:unit"/>
            <xs:element name="value" type="jbs:value"/>
        </xs:sequence>
        <xs:attribute name="name" type="jbs:name"/>
    </xs:complexType>

    <xs:complexType name="Thresholds">
        <xs:sequence>
            <xs:element name="description" type="jbs:description"/>
            <xs:element name="threshold" type="Threshold" minOccurs="0" maxOccurs="unbounded"/>
        </xs:sequence>
        <xs:attribute name="name" type="jbs:name"/>
    </xs:complexType>

    <xs:complexType name="Parameters">
        <xs:sequence>
            <xs:element name="description" type="jbs:description"/>
            <xs:element name="parameter" type="Parameter" minOccurs="0" maxOccurs="unbounded"/>
        </xs:sequence>
        <xs:attribute name="name" type="jbs:name"/>
    </xs:complexType>

    <xs:complexType name="Result" type="jbs:result">
        <xs:sequence>
            <xs:element name="description" type="jbs:description"/>
            <xs:element name="unit" type="jbs:unit"/>
            <xs:element name="boolValue" type="jbs:boolean"/>
            <xs:element name="intValue" type="jbs:integer"/>
            <xs:element name="dblValue" type="jbs:double"/>
            <xs:element name="value" type="jbs:value"/>
            <xs:element name="id" type="jbs:id"/>
            <xs:element name="message" type="jbs:message"/>
            <xs:element name="threshold" type="Threshold" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="thresholds" type="Thresholds" minOccurs="0" maxOccurs="unbounded"/>
        </xs:sequence>
        <xs:attribute name="name" type="jbs:name"/>
    </xs:complexType>

    <xs:complexType name="Test">
        <xs:sequence>
            <xs:element name="description" type="jbs:description"/>
            <xs:element name="result" type="Result" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="parameter" type="Parameter" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="parameters" type="Parameters" minOccurs="0" maxOccurs="unbounded"/>
        </xs:sequence>
        <xs:attribute name="name" type="jbs:name"/>
    </xs:complexType>

    <xs:complexType name="Group">
        <xs:sequence>
            <xs:element name="description" type="jbs:description"/>
            <xs:element name="test" type="Test" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="parameter" type="Parameter" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="parameters" type="Parameters" minOccurs="0" maxOccurs="unbounded"/>
        </xs:sequence>
        <xs:attribute name="name" type="jbs:name"/>
    </xs:complexType>

    <xs:element name="group" type="Group"/>
</xs:schema>
```