package checks.security;

import com.ctc.wstx.shaded.msv.org_isorelax.jaxp.ValidatingDocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

class AllowXMLInclusionCheck {

  static final String X_INCLUDE = "http://apache.org/xml/features/xinclude";
  static final boolean TRUE_VALUE = true;

  void configure(DocumentBuilderFactory factory, boolean unknownValue) {
    factory.setXIncludeAware(true); // Noncompliant [[sc=30;ec=34]] {{Disable the inclusion of files in XML processing.}}
    factory.setXIncludeAware(TRUE_VALUE); // Noncompliant
    factory.setXIncludeAware(false); // Compliant
    factory.setXIncludeAware(unknownValue); // Compliant
  }

  void configure(ValidatingDocumentBuilderFactory factory) {
    factory.setXIncludeAware(true); // Noncompliant
    factory.setXIncludeAware(false); // Compliant
  }

  void configure(SAXParserFactory factory) {
    factory.setXIncludeAware(true); // Noncompliant
    factory.setXIncludeAware(false); // Compliant
  }

  void configure(XMLInputFactory factory, boolean unknownValue) {
    factory.setProperty("http://apache.org/xml/features/xinclude", true); // Noncompliant
    factory.setProperty(X_INCLUDE, TRUE_VALUE); // Noncompliant
    factory.setProperty("http://apache.org/xml/features/xinclude", false); // Compliant
    factory.setProperty(X_INCLUDE, unknownValue); // Compliant
  }

  void configure(TransformerFactory factory) throws TransformerConfigurationException {
    factory.setFeature(X_INCLUDE, TRUE_VALUE); // Noncompliant
    factory.setFeature(X_INCLUDE, false); // Compliant
  }

  void configure(SchemaFactory factory) throws SAXNotSupportedException, SAXNotRecognizedException {
    factory.setFeature(X_INCLUDE, TRUE_VALUE); // Noncompliant
    factory.setFeature(X_INCLUDE, false); // Compliant
  }

  void configure(org.dom4j.io.SAXReader reader) throws SAXException {
    reader.setFeature(X_INCLUDE, TRUE_VALUE); // Noncompliant
    reader.setFeature(X_INCLUDE, false); // Compliant
  }

  void configure(org.jdom2.input.SAXBuilder builder) throws SAXException {
    builder.setFeature(X_INCLUDE, TRUE_VALUE); // Noncompliant
    builder.setFeature(X_INCLUDE, false); // Compliant
  }

}
