import java.io.IOException;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.jdom2.JDOMException;
import org.xml.sax.SAXException;

class SAXReaderTest {

  // Vulnerable when nothing is made to protect against xxe
  SAXReader no_property() {
    SAXReader xmlReader = new SAXReader();  // Noncompliant [[sc=31;ec=40]] {{Disable XML external entity (XXE) processing.}}
    return xmlReader;
  }

  // Securing with features

  SAXReader securing_with_feature() throws SAXException {
    SAXReader xmlReader = new SAXReader();
    xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return xmlReader;
  }

  SAXReader securing_with_feature_2() throws SAXException {
    SAXReader xmlReader = new SAXReader();
    xmlReader.setFeature("http://xml.org/sax/features/external-general-entities", false);
    return xmlReader;
  }

  SAXReader not_securing_with_feature_false() throws SAXException {
    SAXReader xmlReader = new SAXReader(); // Noncompliant
    xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
    return xmlReader;
  }

  SAXReader not_securing_with_feature() throws SAXException {
    SAXReader xmlReader = new SAXReader(); // Noncompliant
    xmlReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    return xmlReader;
  }

  // "universal fix" not supported
  SAXReader univeral_fix() throws SAXException, ParserConfigurationException {
    SAXReader xmlReader = new SAXReader();  // Noncompliant
    xmlReader.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    xmlReader.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return xmlReader;
  }

  // Not returned but used

  void not_returned() throws JDOMException, IOException, DocumentException {
    SAXReader xmlReader = new SAXReader(); // Noncompliant
    Document xmlResponse = xmlReader.read("xxe.xml");
  }

}
