package symbolicexecution.checks;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

public class S6373_AllowXMLInclusionCheck_SAXParserFactory {

  SAXParserFactory x_include_is_false_by_default() throws SAXNotSupportedException, SAXNotRecognizedException, ParserConfigurationException {
    SAXParserFactory factory = SAXParserFactory.newInstance(); // Compliant, xinclude is false by default
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return factory;
  }

  SAXParserFactory x_include_setter_to_false() throws SAXNotSupportedException, SAXNotRecognizedException, ParserConfigurationException {
    SAXParserFactory factory = SAXParserFactory.newInstance(); // Compliant, xinclude is explicitly false
    factory.setXIncludeAware(false);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return factory;
  }

  SAXParserFactory x_include_feature_to_false() throws SAXNotSupportedException, SAXNotRecognizedException, ParserConfigurationException {
    SAXParserFactory factory = SAXParserFactory.newInstance(); // Compliant, xinclude is explicitly false
    factory.setFeature("http://apache.org/xml/features/xinclude", false);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return factory;
  }

  SAXParserFactory x_include_setter_to_true() throws SAXNotSupportedException, SAXNotRecognizedException, ParserConfigurationException {
    SAXParserFactory factory = SAXParserFactory.newInstance();  // Noncompliant [[sc=49;ec=60]] {{Disable the inclusion of files in XML processing.}}
    factory.setXIncludeAware(true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return factory;
  }

  SAXParserFactory x_include_feature_to_true() throws SAXNotSupportedException, SAXNotRecognizedException, ParserConfigurationException {
    SAXParserFactory factory = SAXParserFactory.newInstance();  // Noncompliant
    factory.setFeature("http://apache.org/xml/features/xinclude", true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return factory;
  }

}
