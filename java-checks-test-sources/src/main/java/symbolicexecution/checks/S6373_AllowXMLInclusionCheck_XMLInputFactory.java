package symbolicexecution.checks;

import javax.xml.stream.XMLInputFactory;

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
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Noncompliant [[sc=47;ec=58]] {{Disable the inclusion of files in XML processing.}}
    factory.setProperty("http://apache.org/xml/features/xinclude", true);
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    return factory;
  }

  XMLInputFactory x_include_feature_to_true_without_entity_resolver() {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Noncompliant
    factory.setProperty("http://apache.org/xml/features/xinclude", true);
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

}
