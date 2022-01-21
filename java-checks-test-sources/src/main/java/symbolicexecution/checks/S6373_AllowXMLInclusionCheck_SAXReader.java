package symbolicexecution.checks;

import java.io.File;
import java.io.StringReader;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class S6373_AllowXMLInclusionCheck_SAXReader {

  SAXReader x_include_is_false_by_default() throws SAXException {
    SAXReader xmlReader = new SAXReader(); // Compliant, xinclude is false by default
    xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return xmlReader;
  }

  SAXReader x_include_feature_to_false() throws SAXException {
    SAXReader xmlReader = new SAXReader(); // Compliant, false by default
    xmlReader.setFeature("http://apache.org/xml/features/xinclude", false);
    xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return xmlReader;
  }

  SAXReader x_include_feature_to_true() throws SAXException {
    SAXReader xmlReader = new SAXReader();
    xmlReader.setFeature("http://apache.org/xml/features/xinclude", true); // Noncompliant {{Disable the inclusion of files in XML processing.}}
    xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return xmlReader;
  }

  SAXReader x_include_feature_to_true_without_entity_resolver() throws SAXException {
    SAXReader xmlReader = new SAXReader();
    xmlReader.setFeature("http://apache.org/xml/features/xinclude", true); // Noncompliant
    xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    xmlReader.setEntityResolver(null);
    return xmlReader;
  }

  SAXReader x_include_feature_to_true_with_entity_resolver() throws SAXException {
    SAXReader xmlReader = new SAXReader(); // Compliant with a custom entity resolver
    xmlReader.setFeature("http://apache.org/xml/features/xinclude", true);
    xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    xmlReader.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
    return xmlReader;
  }

  void x_include_feature_to_true_used() throws SAXException, DocumentException {
    SAXReader xmlReader = new SAXReader();
    xmlReader.setFeature("http://apache.org/xml/features/xinclude", true); // Noncompliant {{Disable the inclusion of files in XML processing.}}
    xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    xmlReader.read(new File("f"));
  }

}
