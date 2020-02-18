package checks.security;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.XMLConstants;

class StaxTest {

  XMLInputFactory no_property() {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Noncompliant
    return factory;
  }

  XMLInputFactory unrelated_property() {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Noncompliant
    factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
    return factory;
  }

  XMLInputFactory unrelated_property1() {
    XMLInputFactory factory = XMLInputFactory.newInstance();
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, "false");
    return factory;
  }

  XMLInputFactory unrelated_property2() {
    XMLInputFactory factory = XMLInputFactory.newInstance();// Noncompliant
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, "true");
    return factory;
  }

  XMLInputFactory external_entities_with_false_value() {
    XMLInputFactory factory = XMLInputFactory.newInstance();
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
    return factory;
  }

  XMLInputFactory external_entities_with_true_value() {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Noncompliant
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.TRUE);
    return factory;
  }

  XMLInputFactory dtd_with_false_value() {
    XMLInputFactory factory = XMLInputFactory.newInstance();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    return factory;
  }

  XMLInputFactory dtd_with_primitive_false() {
    XMLInputFactory factory = XMLInputFactory.newInstance();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    return factory;
  }
  XMLInputFactory dtd_with_primitive_false2() {
    XMLInputFactory factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    return factory;
  }
  XMLInputFactory dtd_with_primitive_true() {
    XMLInputFactory factory = XMLInputFactory.newFactory(); // Noncompliant
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, true);
    return factory;
  }
  XMLInputFactory dtd_with_primitive_true2() {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Noncompliant
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, true);
    return factory;
  }

  XMLInputFactory dtd_with_unknown_value(Object value) {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Noncompliant
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, value);
    return factory;
  }

  XMLInputFactory other_call(Object value) {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Noncompliant
    factory.setXMLReporter(null);
    return factory;
  }

  XMLInputFactory two_factory(boolean b) {
    if (b) {
      XMLInputFactory factory = XMLInputFactory.newInstance(); // Noncompliant
      return factory;
    } else {
      XMLInputFactory factory = XMLInputFactory.newInstance();
      factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, "false");
      return factory;
    }
  }

  XMLInputFactory two_factory_assign(boolean b) {
    if (b) {
      XMLInputFactory factory;
      factory = XMLInputFactory.newInstance(); // Noncompliant
      return factory;
    } else {
      XMLInputFactory factory;
      factory = XMLInputFactory.newInstance();
      factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, "false");
      return factory;
    }
  }
}

class StaxTest2 {
  XMLInputFactory factory = XMLInputFactory.newInstance(); // Compliant, reported only when declared inside method

  XMLInputFactory factory_instance(){
    StaxTest2 staxTest = new StaxTest2();
    staxTest.factory = XMLInputFactory.newInstance(); // Compliant
    staxTest.setFactoryProperty();
    return staxTest.factory;
  }

  XMLInputFactory factory_field() {
    factory = XMLInputFactory.newInstance(); // Noncompliant
    return factory;
  }

  XMLInputFactory factory_field2(){
    factory = XMLInputFactory.newInstance(); // Compliant
    this.factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, "false");
    return this.factory;
  }

  XMLInputFactory factory_field3(){
    this.factory = XMLInputFactory.newInstance(); // Compliant
    this.factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, "false");
    return this.factory;
  }

  XMLInputFactory factory_field4(){
    this.factory = XMLInputFactory.newInstance(); // Compliant
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, "false");
    return factory;
  }

  XMLInputFactory factory_field5(){
    factory = XMLInputFactory.newInstance(); // Noncompliant
    setFactoryProperty(); // FP, property is correctly set here
    return factory;
  }

  private void setFactoryProperty() {
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, "false");
  }
}


class SAXParserTest {

  SAXParserFactory no_property() {
    SAXParserFactory factory = SAXParserFactory.newInstance(); // Noncompliant
    return factory;
  }

  SAXParserFactory secure_processing_set_to_true() throws SAXNotSupportedException, SAXNotRecognizedException, ParserConfigurationException {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    return factory;
  }

  SAXParserFactory secure_processing_set_to_false() throws SAXNotSupportedException, SAXNotRecognizedException, ParserConfigurationException {
    SAXParserFactory factory = SAXParserFactory.newInstance(); // Noncompliant
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
    return factory;
  }

  SAXParserFactory secure_processing_with_literal_string_set_to_true() throws SAXNotSupportedException, SAXNotRecognizedException, ParserConfigurationException {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
    return factory;
  }

  SAXParserFactory apache_feature_set_to_true() throws SAXNotSupportedException, SAXNotRecognizedException, ParserConfigurationException {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return factory;
  }

  SAXParserFactory other_feature_set_to_true() throws SAXNotSupportedException, SAXNotRecognizedException, ParserConfigurationException {
    SAXParserFactory factory = SAXParserFactory.newInstance(); // Noncompliant
    factory.setFeature("xxx", true);
    return factory;
  }

  SAXParserFactory other_call() throws SAXNotSupportedException, SAXNotRecognizedException, ParserConfigurationException {
    SAXParserFactory factory = SAXParserFactory.newInstance(); // Noncompliant
    factory.setValidating(true);
    return factory;
  }

  SAXParserFactory two_factory(boolean b) throws SAXNotSupportedException, SAXNotRecognizedException, ParserConfigurationException {
    if (b) {
      SAXParserFactory factory = SAXParserFactory.newInstance(); // Noncompliant
      return factory;
    } else {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      return factory;
    }
  }

}

class XMLReaderTest {

  XMLReader no_property(XMLReaderFactory factory) throws SAXException {
    XMLReader xmlReader = factory.createXMLReader(); // Noncompliant
    return xmlReader;
  }

  XMLReader no_property2(XMLReaderFactory factory) throws SAXException {
    XMLReader xmlReader = factory.createXMLReader();
    xmlReader.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    return xmlReader;
  }

  XMLReader no_property3(XMLReaderFactory factory) throws SAXException {
    XMLReader xmlReader = factory.createXMLReader(); // Noncompliant
    xmlReader.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
    return xmlReader;
  }

  XMLReader two_reader(XMLReaderFactory factory, boolean b) throws SAXException {
    if (b) {
      XMLReader xmlReader = factory.createXMLReader(); // Noncompliant
      return xmlReader;
    } else {
      XMLReader xmlReader = factory.createXMLReader();
      xmlReader.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      return xmlReader;
    }
  }

}

class DocumentBuilderFactoryTest {

  DocumentBuilderFactory no_property() {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Noncompliant
    return factory;
  }

  DocumentBuilderFactory no_property2() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    return factory;
  }

  DocumentBuilderFactory no_property3() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Noncompliant
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
    return factory;
  }

  DocumentBuilderFactory two_factory4(boolean b) throws ParserConfigurationException {
    if (b) {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Noncompliant
      return factory;
    } else {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      return factory;
    }
  }


}

class Foo {
  XMLInputFactory factory = XMLInputFactory.newInstance();
}

class Validator {
  void no_property () throws SAXException  {
    javax.xml.validation.SchemaFactory factory = null;
    javax.xml.validation.Schema schema = factory.newSchema();
    javax.xml.validation.Validator validator = schema.newValidator(); // Noncompliant
  }

  void withAccessExternalDtdAndExternalSchema() throws SAXException  {
    javax.xml.validation.SchemaFactory factory = null;
    javax.xml.validation.Schema schema = factory.newSchema();
    javax.xml.validation.Validator validator = schema.newValidator();
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
  }

  void withAccessExternalDtd() throws SAXException {
    javax.xml.validation.SchemaFactory factory = null;
    javax.xml.validation.Schema schema = factory.newSchema();
    javax.xml.validation.Validator validator = schema.newValidator(); // Noncompliant
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
  }

  void withAccessExternalSchema() throws SAXException {
    javax.xml.validation.SchemaFactory factory = null;
    javax.xml.validation.Schema schema = factory.newSchema();
    javax.xml.validation.Validator validator = schema.newValidator(); // Noncompliant
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
  }

  void withAccessExternalDtdDifferentThatEmptyString() throws SAXException {
    javax.xml.validation.SchemaFactory factory = null;
    javax.xml.validation.Schema schema = factory.newSchema();
    javax.xml.validation.Validator validator = schema.newValidator(); // Noncompliant
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "all");
  }

  void inlined_value () throws SAXException {
    javax.xml.validation.SchemaFactory factory = null;
    javax.xml.validation.Schema schema = factory.newSchema();
    javax.xml.validation.Validator validator = schema.newValidator();
    validator.setProperty("http://javax.xml.XMLConstants/property/accessExternalDTD", "");
    validator.setProperty("http://javax.xml.XMLConstants/property/accessExternalSchema", "");
  }

  void noValidator() throws SAXException {
    javax.xml.validation.SchemaFactory factory = javax.xml.validation.SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    javax.xml.validation.Schema schema = factory.newSchema();
  }

  void otherProperty() throws SAXException {
    javax.xml.validation.SchemaFactory factory = null;
    javax.xml.validation.Schema schema = factory.newSchema();
    javax.xml.validation.Validator validator = schema.newValidator(); // Noncompliant
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, ""); // Coverage: Setting other property other than ACCESS_EXTERNAL_DTD or ACCESS_EXTERNAL_SCHEMA

  }

  void otherMethodCall() throws SAXException  {
    javax.xml.validation.SchemaFactory factory = null;
    javax.xml.validation.Schema schema = factory.newSchema();
    javax.xml.validation.Validator validator = schema.newValidator(); // Noncompliant
    validator.getFeature("something");
  }

  void twoFactory(boolean b) throws SAXException {
    if (b) {
      javax.xml.validation.SchemaFactory factory = null;
      javax.xml.validation.Schema schema = factory.newSchema();
      javax.xml.validation.Validator validator = schema.newValidator(); // Noncompliant
    } else {
      javax.xml.validation.SchemaFactory factory = null;
      javax.xml.validation.Schema schema = factory.newSchema();
      javax.xml.validation.Validator validator = schema.newValidator();
      validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    }
  }

}

interface ValidatorInterface {
  Runnable r = () -> {
    javax.xml.validation.SchemaFactory factory = null;
    javax.xml.validation.Schema schema = null;
    try {
      schema = factory.newSchema();
    } catch (SAXException e) {
      e.printStackTrace();
    }
    javax.xml.validation.Validator validator = schema.newValidator();
  };
}

class TransformerFactoryTest {

  static final boolean BOOLEAN_TRUE = true;

  TransformerFactory classField = TransformerFactory.newInstance();

  TransformerFactory no_call_to_securing_method() {
    TransformerFactory factory = TransformerFactory.newInstance(); // Noncompliant
    return factory;
  }

  TransformerFactory no_call_to_sax_securing_method() {
    TransformerFactory factory = SAXTransformerFactory.newInstance(); // Noncompliant
    return factory;
  }

  // setFeature

  TransformerFactory secure_processing_true() throws TransformerConfigurationException {
    TransformerFactory factory = TransformerFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    return factory;
  }

  TransformerFactory secure_processing_false() throws TransformerConfigurationException {
    TransformerFactory factory = TransformerFactory.newInstance(); // Noncompliant
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
    return factory;
  }

  TransformerFactory secure_processing_true_with_literal() throws TransformerConfigurationException {
    TransformerFactory factory = TransformerFactory.newInstance();
    factory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
    return factory;
  }

  TransformerFactory setfeature_with_other_than_secure_processing() throws TransformerConfigurationException {
    TransformerFactory factory = TransformerFactory.newInstance(); // Noncompliant
    factory.setFeature(XMLConstants.ACCESS_EXTERNAL_DTD, true);
    return factory;
  }

  // setAttribute

  TransformerFactory setattribute_dtd_and_stylesheet() {
    TransformerFactory factory = TransformerFactory.newInstance();
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    return factory;
  }

  TransformerFactory setattribute_dtd_and_other() {
    TransformerFactory factory = TransformerFactory.newInstance(); // Noncompliant
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.DEFAULT_NS_PREFIX, "");
    return factory;
  }

  TransformerFactory setattribute_dtd_and_stylesheet_with_literals() {
    TransformerFactory factory = TransformerFactory.newInstance();
    factory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "");
    factory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalStylesheet", "");
    return factory;
  }

  TransformerFactory setattribute_dtd_only() {
    TransformerFactory factory = TransformerFactory.newInstance(); // Noncompliant
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    return factory;
  }

  TransformerFactory setattribute_stylesheet_only() {
    TransformerFactory factory = TransformerFactory.newInstance(); // Noncompliant
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    return factory;
  }

  TransformerFactory setattribute_dtd_and_stylesheet_with_non_empty_value() {
    TransformerFactory factory = TransformerFactory.newInstance(); // Noncompliant
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "xxx");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    return factory;
  }

  TransformerFactory setattribute_dtd_and_stylesheet_with_unknown_value(String value) {
    TransformerFactory factory = TransformerFactory.newInstance(); // Noncompliant
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, value);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, value);
    return factory;
  }
}

class TransformerFactoryLimitations {

  TransformerFactory multiple_instances() throws TransformerConfigurationException {
    TransformerFactory factory = TransformerFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory = TransformerFactory.newInstance(); // FN
    return factory;
  }

  TransformerFactory cross_procedural() throws TransformerConfigurationException {
    TransformerFactory factory = TransformerFactory.newInstance(); // Noncompliant (FP, would require symbolic execution)
    enableSecureProcessing(factory);
    return factory;
  }

  void enableSecureProcessing(TransformerFactory factory) throws TransformerConfigurationException {
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
  }
}

interface I {
  Runnable r = () -> {
    TransformerFactory factory = TransformerFactory.newInstance();
  };
}
