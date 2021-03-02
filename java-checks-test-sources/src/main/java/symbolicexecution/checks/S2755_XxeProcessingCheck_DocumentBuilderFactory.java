package symbolicexecution.checks;

import java.io.InputStream;
import java.io.StringReader;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import javax.xml.XMLConstants;
import java.io.IOException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import java.io.File;

class DocumentBuilderFactoryTest {
  // Vulnerable when nothing is made to protect against xxe
  DocumentBuilderFactory no_property() {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Noncompliant [[sc=61;ec=72]] {{Disable access to external entities in XML parsing.}}
    return factory;
  }

  DocumentBuilder no_property_builder() throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder(); // Noncompliant
    return builder;
  }

  DocumentBuilderFactory set_feature_secure() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Noncompliant
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true); // No effect to protect against xxe
    return factory;
  }

  // Not vulnerable when "http://apache.org/xml/features/disallow-doctype-decl" set to true
  // or http://apache.org/xml/features/nonvalidating/load-external-dtd set to false
  // or http://xml.org/sax/features/external-general-entities
  DocumentBuilderFactory set_disallow_doctype_true() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Compliant
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return factory;
  }

  DocumentBuilderFactory set_disallow_doctype_false() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Noncompliant
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
    return factory;
  }

  DocumentBuilderFactory set_load_external_false() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Compliant
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    return factory;
  }

  DocumentBuilderFactory set_load_external_true() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Noncompliant
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", true);
    return factory;
  }

  DocumentBuilderFactory set_external_general_true() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Noncompliant
    factory.setFeature("http://xml.org/sax/features/external-general-entities", true);
    return factory;
  }

  DocumentBuilderFactory set_external_general_false() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    return factory;
  }

  void secure_with_no_op_entity_resolver(InputStream is) throws Exception {
    DocumentBuilderFactory df = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder1 = df.newDocumentBuilder(); // Compliant thanks to "builder.setEntityResolver"
    builder1.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
    Document doc1 = builder1.parse(is);
    DocumentBuilder builder2 = df.newDocumentBuilder(); // Noncompliant [[sc=35;ec=53]]
    Document doc2 = builder2.parse(is);
  }

  void insecure_with_null_entity_resolver(InputStream is) throws Exception {
    DocumentBuilderFactory df = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = df.newDocumentBuilder(); // Noncompliant [[sc=34;ec=52]]
    builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
    builder.setEntityResolver(null);
    Document doc = builder.parse(is);
  }

  // "universal fix": ACCESS_EXTERNAL_DTD and ACCESS_EXTERNAL_SCHEMA should be set to ""
  DocumentBuilderFactory univeral_fix() {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return factory;
  }

  DocumentBuilderFactory univeral_fix_only_dtd() {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Noncompliant
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    return factory;
  }

  DocumentBuilderFactory univeral_fix_only_schema() {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Noncompliant
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return factory;
  }

  DocumentBuilderFactory univeral_fix_not_empty() {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Noncompliant
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "all");
    return factory;
  }

  // Directly used without return

  void used_in_method() throws ParserConfigurationException, IOException, SAXException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder(); // Noncompliant
    Document document = builder.parse(new File("xxe.xml"));
  }

  void new_document_in_method() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Compliant
    Document doc = factory.newDocumentBuilder().newDocument();
  }

  Document new_document_returned() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Compliant
    Document doc = factory.newDocumentBuilder().newDocument();
    return doc;
  }

  DocumentBuilder document_builder_returned() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder(); // Noncompliant
    return builder;
  }

  void used_in_try_catch_throw_new(String content) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

      factory.setExpandEntityReferences(true);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); // Compliant when set to true

      DocumentBuilder builder = factory.newDocumentBuilder();
      Document document = builder.parse(new File("xxe.xml"));
    } catch (Exception e) {
      throw new RuntimeException("");
    }
  }

  // Detect issues depending on the symbol
  DocumentBuilderFactory two_factory4(boolean b) throws ParserConfigurationException {
    if (b) {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Noncompliant
      return factory;
    } else {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      return factory;
    }
  }
}
