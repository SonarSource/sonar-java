package symbolicexecution.checks;

import java.io.IOException;
import java.io.StringReader;
import javax.xml.XMLConstants;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

class XmlReaderTest {

  // Vulnerable when nothing is made to protect against xxe

  XMLReader no_property(XMLReaderFactory factory) throws SAXException {
    XMLReader xmlReader = factory.createXMLReader(); // Noncompliant
    return xmlReader;
  }

  // Test securing methods with features

  XMLReader secure_with_feature_1(XMLReaderFactory factory) throws SAXException {
    XMLReader xmlReader = factory.createXMLReader(); // Compliant
    xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return xmlReader;
  }

  XMLReader secure_with_feature_2(XMLReaderFactory factory) throws SAXException {
    XMLReader xmlReader = factory.createXMLReader(); // Compliant
    xmlReader.setFeature("http://xml.org/sax/features/external-general-entities", false);
    return xmlReader;
  }

  XMLReader unsecure_with_feature_1(XMLReaderFactory factory) throws SAXException {
    XMLReader xmlReader = factory.createXMLReader(); // Noncompliant
    xmlReader.setFeature("http://xml.org/sax/features/external-general-entities", true);
    return xmlReader;
  }

  XMLReader unsecure_with_feature_2(XMLReaderFactory factory) throws SAXException {
    XMLReader xmlReader = factory.createXMLReader(); // Noncompliant
    xmlReader.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true); // Do not protect against XXE.
    return xmlReader;
  }

  // Test securing methods with properties

  XMLReader secure_with_properties_1(XMLReaderFactory factory) throws SAXException {
    XMLReader xmlReader = factory.createXMLReader(); // Compliant
    xmlReader.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    xmlReader.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return xmlReader;
  }

  XMLReader unsecure_with_properties_1(XMLReaderFactory factory) throws SAXException {
    XMLReader xmlReader = factory.createXMLReader(); // Noncompliant
    xmlReader.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return xmlReader;
  }

  XMLReader unsecure_with_properties_2(XMLReaderFactory factory) throws SAXException {
    XMLReader xmlReader = factory.createXMLReader(); // Noncompliant
    xmlReader.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    return xmlReader;
  }

  XMLReader unsecure_with_properties_3(XMLReaderFactory factory) throws SAXException {
    XMLReader xmlReader = factory.createXMLReader(); // Noncompliant
    xmlReader.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "all");
    xmlReader.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return xmlReader;
  }

  // Test securing methods with customized EntityResolver

  XMLReader secure_with_no_op_entity_resolver(XMLReaderFactory factory) throws SAXException {
    XMLReader xmlReader = factory.createXMLReader(); // Compliant
    xmlReader.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
    return xmlReader;
  }

  // Directly used without return

  void used_directly(XMLReaderFactory factory) throws SAXException, IOException {
    XMLReader xmlReader = factory.createXMLReader(); // Noncompliant
    xmlReader.parse("xxe.xml");
  }

  XMLReader two_reader(XMLReaderFactory factory, boolean b) throws SAXException {
    if (b) {
      XMLReader xmlReader = factory.createXMLReader(); // Noncompliant
      return xmlReader;
    } else {
      XMLReader xmlReader = factory.createXMLReader();
      xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      return xmlReader;
    }
  }

}
