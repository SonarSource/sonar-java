package symbolicexecution.checks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

class DocumentBuilderFactoryTest {
  // Vulnerable when nothing is made to protect against xxe
  DocumentBuilderFactory no_property() {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Noncompliant {{Disable access to external entities in XML parsing.}}
//                                                          ^^^^^^^^^^^
    return factory;
  }

  DocumentBuilder no_property_builder() throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Noncompliant
    DocumentBuilder builder = factory.newDocumentBuilder();
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
    DocumentBuilderFactory df = DocumentBuilderFactory.newInstance(); // FN: one path is not secured
    DocumentBuilder builder1 = df.newDocumentBuilder(); // Compliant thanks to "builder.setEntityResolver"
    builder1.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
    Document doc1 = builder1.parse(is);
    DocumentBuilder builder2 = df.newDocumentBuilder(); // No entity resolver set
    Document doc2 = builder2.parse(is);
  }

  void insecure_with_null_entity_resolver(InputStream is) throws Exception {
    DocumentBuilderFactory df = DocumentBuilderFactory.newInstance(); // Noncompliant
//                                                     ^^^^^^^^^^^
    DocumentBuilder builder = df.newDocumentBuilder();
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

  // Secure with "setExpandEntityReferences"
  DocumentBuilderFactory secure_with_set_expand_entity_references_false() {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Compliant, when the java version is not explicitly set, we accept the fix
    factory.setExpandEntityReferences(false);
    return factory;
  }

  DocumentBuilderFactory secure_with_set_expand_entity_references_true() {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Noncompliant
    factory.setExpandEntityReferences(true);
    return factory;
  }

  // Directly used without return

  void used_in_method() throws ParserConfigurationException, IOException, SAXException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Noncompliant
    DocumentBuilder builder = factory.newDocumentBuilder();
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
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Noncompliant
    DocumentBuilder builder = factory.newDocumentBuilder();
    return builder;
  }

  void used_in_try_catch_throw_new(String content) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

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

class DocumentBuilderFactoryTest_InStaticBlock {
  private static final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

  static {
    documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
  }

  public static DocumentBuilder noIssue() throws Exception {
    // No issue here, the code is equivalent to the one in the next method.
    // We only report an issue on the declaration of "DocumentBuilderFactory", exactly to avoid such cases where the DocumentBuilder is created somewhere else.
    return documentBuilderFactory.newDocumentBuilder();
  }

  public static DocumentBuilder equivalent() throws Exception {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return documentBuilderFactory.newDocumentBuilder();
  }
}
