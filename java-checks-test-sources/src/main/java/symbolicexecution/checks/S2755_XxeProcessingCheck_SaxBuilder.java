package symbolicexecution.checks;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

class SAXBuilderTest {

  // Vulnerable when nothing is made to protect against xxe
  SAXBuilder no_property() {
    SAXBuilder builder = new SAXBuilder(); // Noncompliant [[sc=30;ec=40]]
    return builder;
  }

  // Securing with features

  SAXBuilder securing_with_feature() {
    SAXBuilder builder = new SAXBuilder(); // Compliant
    builder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return builder;
  }

  SAXBuilder not_securing_with_feature_false() {
    SAXBuilder builder = new SAXBuilder(); // Noncompliant
    builder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
    return builder;
  }

  SAXBuilder not_securing_with_feature() {
    SAXBuilder builder = new SAXBuilder(); // Noncompliant
    builder.setFeature("http://xml.org/sax/features/external-parameter-entities", false); // No effect
    return builder;
  }

  // "universal fix": ACCESS_EXTERNAL_DTD and ACCESS_EXTERNAL_SCHEMA should be set to ""
  SAXBuilder univeral_fix() throws SAXException, ParserConfigurationException {
    SAXBuilder builder = new SAXBuilder(); // Compliant
    builder.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    builder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return builder;
  }

  SAXBuilder univeral_fix_only_dtd() throws SAXException, ParserConfigurationException {
    SAXBuilder builder = new SAXBuilder(); // Noncompliant
    builder.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    return builder;
  }

  SAXBuilder univeral_fix_only_schema() throws SAXException, ParserConfigurationException {
    SAXBuilder builder = new SAXBuilder(); // Noncompliant
    builder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return builder;
  }

  SAXBuilder univeral_fix_not_empty() throws SAXException, ParserConfigurationException {
    SAXBuilder builder = new SAXBuilder(); // Noncompliant
    builder.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "all");
    builder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "all");
    return builder;
  }

  // Test securing methods with customized EntityResolver

  SAXBuilder secure_with_no_op_entity_resolver() throws SAXException, ParserConfigurationException {
    SAXBuilder builder = new SAXBuilder(); // Compliant
    builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
    return builder;
  }

  // Not returned but used

  void not_returned() throws JDOMException, IOException {
    SAXBuilder builder = new SAXBuilder(); // Noncompliant
    Document document = builder.build(new File("xxe.xml"));
  }

}
