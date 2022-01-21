package symbolicexecution.checks;

import com.ctc.wstx.shaded.msv.org_isorelax.jaxp.ValidatingDocumentBuilderFactory;
import com.ctc.wstx.shaded.msv.org_isorelax.verifier.Schema;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class S6373_AllowXMLInclusionCheck_DocumentBuilderFactory {

  DocumentBuilderFactory x_include_is_false_by_default() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Compliant, xinclude is false by default
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return factory;
  }

  DocumentBuilderFactory x_include_setter_to_false() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Compliant, xinclude is explicitly false
    factory.setXIncludeAware(false);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return factory;
  }

  DocumentBuilderFactory x_include_setter_to_unknown(boolean unknown) throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setXIncludeAware(unknown); // Compliant
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return factory;
  }

  DocumentBuilderFactory x_include_feature_to_false() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Compliant, xinclude is explicitly false
    factory.setFeature("http://apache.org/xml/features/xinclude", false);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return factory;
  }

  DocumentBuilderFactory x_include_setter_to_true() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setXIncludeAware(true); // Noncompliant [[sc=5;ec=35]] {{Disable the inclusion of files in XML processing.}}
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return factory;
  }

  DocumentBuilderFactory x_include_feature_to_true() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/xinclude", true); // Noncompliant {{Disable the inclusion of files in XML processing.}}
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return factory;
  }

  ValidatingDocumentBuilderFactory x_include_setter_to_true_on_subtype_of_DocumentBuilderFactory(Schema schema) throws ParserConfigurationException {
    ValidatingDocumentBuilderFactory factory = ValidatingDocumentBuilderFactory.newInstance(schema);
    factory.setXIncludeAware(true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return factory;
  }

  Document x_include_setter_to_true_without_entity_resolver(InputStream is) throws ParserConfigurationException, IOException, SAXException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setXIncludeAware(true); // Noncompliant [[sc=5;ec=35]]
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    return builder.parse(is);
  }

  Document x_include_setter_to_true_with_entity_resolver_null(InputStream is) throws ParserConfigurationException, IOException, SAXException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setXIncludeAware(true); // Noncompliant
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    builder.setEntityResolver(null);
    return builder.parse(is);
  }

  Document x_include_setter_to_true_with_entity_resolver(InputStream is) throws ParserConfigurationException, IOException, SAXException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setXIncludeAware(true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    DocumentBuilder builder = factory.newDocumentBuilder(); // Compliant with a custom entity resolver
    builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
    return builder.parse(is);
  }

}
