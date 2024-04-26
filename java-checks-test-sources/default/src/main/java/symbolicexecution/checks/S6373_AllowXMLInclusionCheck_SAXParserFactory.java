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
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setXIncludeAware(true); // Noncompliant {{Disable the inclusion of files in XML processing.}}
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return factory;
  }

  SAXParserFactory x_include_feature_to_true() throws SAXNotSupportedException, SAXNotRecognizedException, ParserConfigurationException {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/xinclude", true); // Noncompliant
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return factory;
  }

}
