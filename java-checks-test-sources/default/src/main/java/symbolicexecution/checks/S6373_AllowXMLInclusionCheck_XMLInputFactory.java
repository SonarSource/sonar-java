package symbolicexecution.checks;

import java.io.Reader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class S6373_AllowXMLInclusionCheck_XMLInputFactory {

  XMLInputFactory x_include_is_false_by_default() {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Compliant, xinclude is false by default
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    return factory;
  }

  XMLInputFactory x_include_feature_to_false() {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Compliant, xinclude is explicitly false
    factory.setProperty("http://apache.org/xml/features/xinclude", false);
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    return factory;
  }

  XMLInputFactory x_include_feature_to_true() {
    XMLInputFactory factory = XMLInputFactory.newInstance();
    factory.setProperty("http://apache.org/xml/features/xinclude", true); // Noncompliant {{Disable the inclusion of files in XML processing.}}
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    return factory;
  }

  XMLInputFactory x_include_feature_to_true_without_entity_resolver() {
    XMLInputFactory factory = XMLInputFactory.newInstance();
    factory.setProperty("http://apache.org/xml/features/xinclude", true); // Noncompliant
    factory.setXMLResolver(null);
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    return factory;
  }

  XMLInputFactory x_include_feature_to_true_with_entity_resolver() {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Compliant with a custom entity resolver
    factory.setProperty("http://apache.org/xml/features/xinclude", true);
    factory.setXMLResolver(NoopXMLResolver.create());
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    return factory;
  }

  XMLStreamReader partial_configuration(XMLInputFactory factory, Reader reader) throws XMLStreamException {
    factory.setProperty("http://apache.org/xml/features/xinclude", true);
    return factory.createXMLStreamReader(reader);
  }

}
