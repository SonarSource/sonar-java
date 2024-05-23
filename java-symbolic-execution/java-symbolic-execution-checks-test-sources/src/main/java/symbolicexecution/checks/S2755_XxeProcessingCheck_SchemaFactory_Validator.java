package symbolicexecution.checks;

import java.io.IOException;
import java.io.StringWriter;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.SAXException;

class SchemaFactory_Validator {

  // Vulnerable when nothing is made to protect against xxe
  Validator no_property () throws SAXException  {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); // Noncompliant {{Disable access to external entities in XML parsing.}}
    Schema schema = schemaFactory.newSchema();
    Validator validator = schema.newValidator();
    return validator;
  }

  Validator new_schema_with_argument () throws SAXException  {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); // Noncompliant
    StreamSource xsdStreamSource = new StreamSource("xxe.xsd");
    Schema schema = schemaFactory.newSchema(xsdStreamSource);
    Validator validator = schema.newValidator();
    return validator;
  }

  // Securing at validator level
  Validator withAccessExternalDtdAndExternalSchema_validator() throws SAXException  {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); // Compliant
    Schema schema = schemaFactory.newSchema();
    Validator validator = schema.newValidator();
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return validator;
  }

  Validator inlined_value_validator () throws SAXException {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    Schema schema = schemaFactory.newSchema();
    Validator validator = schema.newValidator();
    validator.setProperty("http://javax.xml.XMLConstants/property/accessExternalDTD", "");
    validator.setProperty("http://javax.xml.XMLConstants/property/accessExternalSchema", "");
    return validator;
  }

  Validator withAccessExternalDtd_validator() throws SAXException {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); // Noncompliant
    Schema schema = schemaFactory.newSchema();
    Validator validator = schema.newValidator();
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    return validator;
  }

  Validator withAccessExternalSchema_validator() throws SAXException {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); // Noncompliant
    Schema schema = schemaFactory.newSchema();
    Validator validator = schema.newValidator();
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return validator;
  }

  Validator withAccessExternalDtdDifferentThatEmptyString_validator() throws SAXException {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); // Noncompliant
    Schema schema = schemaFactory.newSchema();
    Validator validator = schema.newValidator();
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "all");
    return validator;
  }

  Validator otherProperty_validator() throws SAXException {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); // Noncompliant
    Schema schema = schemaFactory.newSchema();
    Validator validator = schema.newValidator();
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    return validator;
  }

  Validator otherMethodCall_validator() throws SAXException  {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); // Noncompliant
    Schema schema = schemaFactory.newSchema();
    Validator validator = schema.newValidator();
    validator.getFeature("something");
    return validator;
  }

  // Securing at schema level

  Validator withAccessExternalDtdAndExternalSchema() throws SAXException  {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); // Compliant
    schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

    Schema schema = schemaFactory.newSchema();
    Validator validator = schema.newValidator();
    return validator;
  }

  Validator withAccessExternalDtd() throws SAXException  {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); // Noncompliant
    schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");

    Schema schema = schemaFactory.newSchema();
    Validator validator = schema.newValidator();
    return validator;
  }

  Validator withAccessExternalSchema() throws SAXException  {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); // Noncompliant
    schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

    Schema schema = schemaFactory.newSchema();
    Validator validator = schema.newValidator();
    return validator;
  }

  Validator withAccessNotEmptyString() throws SAXException  {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); // Noncompliant
    schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "all");

    Schema schema = schemaFactory.newSchema();
    Validator validator = schema.newValidator();
    return validator;
  }

  // Mixed schema and validator

  Validator mixedSchemaValidator() throws SAXException  {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); // Compliant
    schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    Schema schema = schemaFactory.newSchema();
    Validator validator = schema.newValidator();
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return validator;
  }

  // Not returned but used

  void not_returned (StreamSource xmlStreamSource) throws SAXException, IOException  {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); // Noncompliant
    Schema schema = schemaFactory.newSchema();
    Validator validator = schema.newValidator();

    StringWriter writer = new StringWriter();
    validator.validate(xmlStreamSource);
  }

  void not_returned_with_two_args (StreamSource xmlStreamSource) throws SAXException, IOException  {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); // Noncompliant
    Schema schema = schemaFactory.newSchema();
    Validator validator = schema.newValidator();

    StringWriter writer = new StringWriter();
    validator.validate(xmlStreamSource, new StreamResult(writer));
  }

  // Detect issues depending on the symbol

  Validator twoFactory(boolean b) throws SAXException {
    if (b) {
      SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); // Noncompliant
      Schema schema = schemaFactory.newSchema();
      Validator validator = schema.newValidator();
      return validator;
    } else {
      SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); // Compliant
      Schema schema = schemaFactory.newSchema();
      Validator validator = schema.newValidator();
      validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      return validator;
    }
  }
}
