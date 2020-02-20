import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import javax.xml.XMLConstants;
import java.io.IOException;
import org.xml.sax.SAXException;
import java.io.File;

class DocumentBuilderFactoryTest {
  // Vulnerable when nothing is made to protect against xxe
  DocumentBuilderFactory no_property() {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Noncompliant {{Disable XML external entity (XXE) processing.}}
    return factory;
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
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Noncompliant
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document document = builder.parse(new File("xxe.xml"));
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
