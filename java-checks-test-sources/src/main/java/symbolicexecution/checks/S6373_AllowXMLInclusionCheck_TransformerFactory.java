package symbolicexecution.checks;

import javax.xml.XMLConstants;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;

public class S6373_AllowXMLInclusionCheck_TransformerFactory {

  TransformerFactory x_include_is_false_by_default() {
    TransformerFactory factory = TransformerFactory.newInstance(); // Compliant, xinclude is false by default
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    return factory;
  }

  TransformerFactory x_include_feature_to_false() throws TransformerConfigurationException {
    TransformerFactory factory = TransformerFactory.newInstance(); // Compliant, xinclude is explicitly false
    factory.setFeature("http://apache.org/xml/features/xinclude", false);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    return factory;
  }

  TransformerFactory x_include_feature_to_true() throws TransformerConfigurationException {
    TransformerFactory factory = TransformerFactory.newInstance(); // Noncompliant [[sc=53;ec=64]] {{Disable the inclusion of files in XML processing.}}
    factory.setFeature("http://apache.org/xml/features/xinclude", true);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    return factory;
  }

}
