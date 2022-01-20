package symbolicexecution.checks;

import java.io.IOException;
import java.io.InputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.jdom2.input.SAXBuilder;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

public class DenialOfServiceXMLCheck {

  DocumentBuilderFactory factoryAsField;

  // ========== DocumentBuilderFactory ==========

  DocumentBuilderFactory no_property_new_instance() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();  // Compliant, secured by default
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false); // To make XxeProcessingCheck secured

    return factory;
  }

  DocumentBuilderFactory secure_processing_false() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false); // To make XxeProcessingCheck secured

    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false); // Noncompliant [[sc=5;ec=70]] {{Enable XML parsing limitations to prevent Denial of Service attacks.}}
    return factory;
  }

  DocumentBuilderFactory secure_processing_true() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Compliant
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false); // To make XxeProcessingCheck secured

    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    return factory;
  }

  DocumentBuilderFactory secure_processing_false_with_disallow_doctype() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Compliant, when disallow-doctype-decl is set to true
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false); // To make XxeProcessingCheck secured

    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return factory;
  }

  DocumentBuilderFactory secure_processing_false_with_disallow_doctype_false() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false); // To make XxeProcessingCheck secured

    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false); // Noncompliant {{Enable XML parsing limitations to prevent Denial of Service attacks.}}
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
    return factory;
  }

  DocumentBuilderFactory entity_resolver_has_no_effect() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false); // To make XxeProcessingCheck secured

    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false); // Noncompliant {{Enable XML parsing limitations to prevent Denial of Service attacks.}}
    DocumentBuilder builder = factory.newDocumentBuilder();
    builder.setEntityResolver(new MyEntityResolver());
    return factory;
  }

  void new_instance_used(InputStream is) throws Exception {
    DocumentBuilderFactory df = DocumentBuilderFactory.newInstance();
    df.setFeature("http://xml.org/sax/features/external-general-entities", false); // To make XxeProcessingCheck secured
    DocumentBuilder builder = df.newDocumentBuilder();
    Document doc1 = builder.parse(is);
  }

  void secure_processing_true_used(InputStream is) throws Exception {
    DocumentBuilderFactory df = DocumentBuilderFactory.newInstance();
    df.setFeature("http://xml.org/sax/features/external-general-entities", false); // To make XxeProcessingCheck secured

    df.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    DocumentBuilder builder = df.newDocumentBuilder();
    Document doc1 = builder.parse(is);
  }

  void secure_processing_false_used(InputStream is) throws Exception {
    DocumentBuilderFactory df = DocumentBuilderFactory.newInstance();
    df.setFeature("http://xml.org/sax/features/external-general-entities", false); // To make XxeProcessingCheck secured

    df.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false); // Noncompliant {{Enable XML parsing limitations to prevent Denial of Service attacks.}}
    DocumentBuilder builder = df.newDocumentBuilder();
    Document doc1 = builder.parse(is);
  }

  DocumentBuilderFactory fields_are_not_reported() throws ParserConfigurationException {
    factoryAsField = DocumentBuilderFactory.newInstance();
    factoryAsField.setFeature("http://xml.org/sax/features/external-general-entities", false); // To make XxeProcessingCheck secured

    factoryAsField.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
    return factoryAsField;
  }

  // ========== SAXParserFactory ==========

  SAXParserFactory sax_parser_new_instance() throws ParserConfigurationException, SAXNotRecognizedException, SAXNotSupportedException {
    SAXParserFactory factory = SAXParserFactory.newInstance(); // Compliant
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false); // To make XxeProcessingCheck secured

    return factory;
  }

  SAXParserFactory sax_parser_unsecured() throws ParserConfigurationException, SAXNotRecognizedException, SAXNotSupportedException {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false); // To make XxeProcessingCheck secured

    factory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", false); // Noncompliant [[sc=5;ec=89]] {{Enable XML parsing limitations to prevent Denial of Service attacks.}}
    return factory;
  }

  // ========== SchemaFactory  ==========
  Validator schema_factory_new_instance() throws SAXException {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, ""); // To make XxeProcessingCheck secured
    schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); // To make XxeProcessingCheck secured

    Schema schema = schemaFactory.newSchema();
    Validator validator = schema.newValidator();
    return validator;
  }

  Validator schema_factory_unsecured() throws SAXException {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, ""); // To make XxeProcessingCheck secured
    schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); // To make XxeProcessingCheck secured

    schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false); // Noncompliant {{Enable XML parsing limitations to prevent Denial of Service attacks.}}

    Schema schema = schemaFactory.newSchema();
    Validator validator = schema.newValidator();
    return validator;
  }

  // ========== TransformerFactory  ==========
  TransformerFactory transformer_factory_new_instance() {
    TransformerFactory factory = javax.xml.transform.TransformerFactory.newInstance();
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); // To make XxeProcessingCheck secured
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, ""); // To make XxeProcessingCheck secured

    return factory;
  }

  TransformerFactory transformer_factory_unsecured() throws TransformerConfigurationException {
    TransformerFactory factory = TransformerFactory.newInstance();
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); // To make XxeProcessingCheck secured
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, ""); // To make XxeProcessingCheck secured

    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false); // Noncompliant {{Enable XML parsing limitations to prevent Denial of Service attacks.}}
    return factory;
  }

  // ========== SAXReader  ==========
  SAXReader sax_reader_new_instance() throws SAXException {
    SAXReader saxer = new SAXReader();
    saxer.setFeature("http://xml.org/sax/features/external-general-entities", false); // To make XxeProcessingCheck secured

    return saxer;
  }

  SAXReader sax_reader_unsecured() throws SAXException {
    SAXReader saxer = new SAXReader();
    saxer.setFeature("http://xml.org/sax/features/external-general-entities", false); // To make XxeProcessingCheck secured

    saxer.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false); // Noncompliant {{Enable XML parsing limitations to prevent Denial of Service attacks.}}
    return saxer;
  }

  void sax_reader_new_instance_used() throws SAXException, DocumentException {
    SAXReader saxer = new SAXReader();
    saxer.setFeature("http://xml.org/sax/features/external-general-entities", false); // To make XxeProcessingCheck secured

    org.dom4j.Document xmlResponse = saxer.read("something.xml");
  }

  void sax_reader_unsecured_used() throws SAXException, DocumentException {
    SAXReader saxer = new SAXReader();
    saxer.setFeature("http://xml.org/sax/features/external-general-entities", false); // To make XxeProcessingCheck secured

    saxer.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false); // Noncompliant {{Enable XML parsing limitations to prevent Denial of Service attacks.}}
    org.dom4j.Document xmlResponse = saxer.read("something.xml");
  }

  // ========== SAXBuilder  ==========
  SAXBuilder sax_builder_new_instance() {
    SAXBuilder builder = new SAXBuilder();
    builder.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, ""); // To make XxeProcessingCheck secured
    builder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); // To make XxeProcessingCheck secured

    return builder;
  }

  SAXBuilder sax_builder_unsecured() {
    SAXBuilder builder = new SAXBuilder();
    builder.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, ""); // To make XxeProcessingCheck secured
    builder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); // To make XxeProcessingCheck secured

    builder.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false); // Noncompliant {{Enable XML parsing limitations to prevent Denial of Service attacks.}}
    return builder;
  }

  SAXBuilder sax_builder_unsecured_2() {
    SAXBuilder builder = new SAXBuilder();
    builder.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, ""); // To make XxeProcessingCheck secured
    builder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); // To make XxeProcessingCheck secured

    builder.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false); // Noncompliant {{Enable XML parsing limitations to prevent Denial of Service attacks.}}
    builder.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false); // Compliant, only report the first
    return builder;
  }

  SAXBuilder sax_builder_unsecured_3() {
    SAXBuilder builder = new SAXBuilder();
    builder.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, ""); // To make XxeProcessingCheck secured
    builder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); // To make XxeProcessingCheck secured

    builder.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
    builder.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    builder.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false); // Noncompliant {{Enable XML parsing limitations to prevent Denial of Service attacks.}}
    return builder;
  }

  SAXBuilder sax_builder_unsecured_4(boolean cond) {
    SAXBuilder builder = new SAXBuilder();
    builder.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, ""); // To make XxeProcessingCheck secured
    builder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); // To make XxeProcessingCheck secured

    // Two end of execution, but only one issue
    builder.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false); // Noncompliant {{Enable XML parsing limitations to prevent Denial of Service attacks.}}
    if (cond) {
      return builder;
    } else {
      System.out.println("Do something else");
      return builder;
    }
  }

  SAXBuilder sax_builder_unsecured_5(boolean cond) {
    SAXBuilder builder = new SAXBuilder();
    builder.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, ""); // To make XxeProcessingCheck secured
    builder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); // To make XxeProcessingCheck secured

    if (cond) {
      builder.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false); // Noncompliant {{Enable XML parsing limitations to prevent Denial of Service attacks.}}
      return builder;
    } else {
      System.out.println("Do something else");
      builder.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false); // Noncompliant {{Enable XML parsing limitations to prevent Denial of Service attacks.}}
      return builder;
    }
  }

}

class MyEntityResolver implements EntityResolver {
  @Override
  public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
    return null;
  }
}
