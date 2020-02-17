import javax.xml.stream.XMLInputFactory;
import javax.xml.parsers.SAXParserFactory;
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

  XMLInputFactory unrelated_property() {
    XMLInputFactory factory = XMLInputFactory.newInstance();
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, "false");
    return factory;
  }

  XMLInputFactory unrelated_property() {
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
    return staxTest.factory;
  }

  XMLInputFactory factory_field2(){
    factory = XMLInputFactory.newInstance(); // Compliant
    this.factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, "false");
    return staxTest.factory;
  }

  XMLInputFactory factory_field3(){
    this.factory = XMLInputFactory.newInstance(); // Compliant
    this.factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, "false");
    return staxTest.factory;
  }

  XMLInputFactory factory_field4(){
    this.factory = XMLInputFactory.newInstance(); // Compliant
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, "false");
    return staxTest.factory;
  }

  XMLInputFactory factory_field5(){
    factory = XMLInputFactory.newInstance(); // Noncompliant
    setFactoryProperty(); // FP, property is correctly set here
    return staxTest.factory;
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

  SAXParserFactory secure_processing_set_to_true() {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    return factory;
  }

  SAXParserFactory secure_processing_set_to_false() {
    SAXParserFactory factory = SAXParserFactory.newInstance(); // Noncompliant
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
    return factory;
  }

  SAXParserFactory secure_processing_with_literal_string_set_to_true() {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
    return factory;
  }

  SAXParserFactory apache_feature_set_to_true() {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return factory;
  }

  SAXParserFactory other_feature_set_to_true() {
    SAXParserFactory factory = SAXParserFactory.newInstance(); // Noncompliant
    factory.setFeature("xxx", true);
    return factory;
  }

  SAXParserFactory two_factory(boolean b) {
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

  XMLReader no_property(XMLReaderFactory factory) {
    XMLReader xmlReader = factory.createXMLReader(); // Noncompliant
    return xmlReader;
  }

  XMLReader no_property(XMLReaderFactory factory) {
    XMLReader xmlReader = factory.createXMLReader();
    xmlReader.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    return xmlReader;
  }

  XMLReader no_property(XMLReaderFactory factory) {
    XMLReader xmlReader = factory.createXMLReader(); // Noncompliant
    xmlReader.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
    return xmlReader;
  }

  XMLReader two_reader(XMLReaderFactory factory, boolean b) {
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

  DocumentBuilderFactory no_property() {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    return factory;
  }

  DocumentBuilderFactory no_property() {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Noncompliant
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
    return factory;
  }

  DocumentBuilderFactory two_factory(boolean b) {
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
  void no_property () {
    javax.xml.validation.SchemaFactory factory;
    javax.xml.validation.Schema schema = factory.newSchema();
    javax.xml.validation.Validator validator = schema.newValidator(); // Noncompliant
  }

  void withAccessExternalDtdAndExternalSchema() {
    javax.xml.validation.SchemaFactory factory;
    javax.xml.validation.Schema schema = factory.newSchema();
    javax.xml.validation.Validator validator = schema.newValidator();
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
  }

  void withAccessExternalDtd() {
    javax.xml.validation.SchemaFactory factory;
    javax.xml.validation.Schema schema = factory.newSchema();
    javax.xml.validation.Validator validator = schema.newValidator(); // Noncompliant
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
  }

  void withAccessExternalSchema() {
    javax.xml.validation.SchemaFactory factory;
    javax.xml.validation.Schema schema = factory.newSchema();
    javax.xml.validation.Validator validator = schema.newValidator(); // Noncompliant
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
  }

  void withAccessExternalDtdDifferentThatEmptyString() {
    javax.xml.validation.SchemaFactory factory;
    javax.xml.validation.Schema schema = factory.newSchema();
    javax.xml.validation.Validator validator = schema.newValidator(); // Noncompliant
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "all");
  }

  void inlined_value () {
    javax.xml.validation.SchemaFactory factory;
    javax.xml.validation.Schema schema = factory.newSchema();
    javax.xml.validation.Validator validator = schema.newValidator();
    validator.setProperty("http://javax.xml.XMLConstants/property/accessExternalDTD", "");
    validator.setProperty("http://javax.xml.XMLConstants/property/accessExternalSchema", "");
  }

  void noValidator() {
    javax.xml.validation.SchemaFactory factory = javax.xml.validation.SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    javax.xml.validation.Schema schema = factory.newSchema();
  }

  void otherProperty() {
    javax.xml.validation.SchemaFactory factory;
    javax.xml.validation.Schema schema = factory.newSchema();
    javax.xml.validation.Validator validator = schema.newValidator(); // Noncompliant
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, ""); // Coverage: Setting other property other than ACCESS_EXTERNAL_DTD or ACCESS_EXTERNAL_SCHEMA

  }

  void twoFactory(boolean b) {
    if (b) {
      javax.xml.validation.SchemaFactory factory;
      javax.xml.validation.Schema schema = factory.newSchema();
      javax.xml.validation.Validator validator = schema.newValidator(); // Noncompliant
    } else {
      javax.xml.validation.SchemaFactory factory;
      javax.xml.validation.Schema schema = factory.newSchema();
      javax.xml.validation.Validator validator = schema.newValidator();
      validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    }
  }

}
interface I {
  Runnable r = () -> {
    javax.xml.validation.SchemaFactory factory;
    javax.xml.validation.Schema schema = factory.newSchema();
    javax.xml.validation.Validator validator = schema.newValidator();
  };
}
