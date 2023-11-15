package symbolicexecution.checks;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.XMLConstants;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamSource;

class TransformerFactoryTest {

  static final boolean BOOLEAN_TRUE = true;

  TransformerFactory classField = TransformerFactory.newInstance();

  TransformerFactory no_call_to_securing_method() {
    TransformerFactory factory = TransformerFactory.newInstance(); // Noncompliant {{Disable access to external entities in XML parsing.}}
    return factory;
  }

  TransformerFactory no_call_to_sax_securing_method() {
    TransformerFactory factory = SAXTransformerFactory.newInstance(); // Noncompliant
    return factory;
  }

  // setFeature has no effect to protect against XXE

  TransformerFactory secure_processing_true() throws TransformerConfigurationException {
    TransformerFactory factory = TransformerFactory.newInstance(); // Noncompliant
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    return factory;
  }

  // setAttribute

  TransformerFactory setattribute_dtd_and_stylesheet() {
    TransformerFactory factory = TransformerFactory.newInstance();
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    return factory;
  }

  TransformerFactory setattribute_dtd_and_other() {
    TransformerFactory factory = TransformerFactory.newInstance(); // Noncompliant
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.DEFAULT_NS_PREFIX, "");
    return factory;
  }

  TransformerFactory setattribute_dtd_and_stylesheet_with_literals() {
    TransformerFactory factory = TransformerFactory.newInstance();
    factory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "");
    factory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalStylesheet", "");
    return factory;
  }

  TransformerFactory setattribute_dtd_only() {
    TransformerFactory factory = TransformerFactory.newInstance(); // Noncompliant
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    return factory;
  }

  TransformerFactory setattribute_stylesheet_only() {
    TransformerFactory factory = TransformerFactory.newInstance(); // Noncompliant
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    return factory;
  }

  TransformerFactory setattribute_dtd_and_stylesheet_with_non_empty_value() {
    TransformerFactory factory = TransformerFactory.newInstance(); // Noncompliant [[flows=stylesheet]]
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "xxx"); // flow@stylesheet [[sc=5;ec=73]] {{Implies 'factory' is unsecured. Set to "" (empty string) to protect against XXE.}}
    return factory;
  }

  TransformerFactory setattribute_dtd_and_stylesheet_with_unknown_value(String value) {
    TransformerFactory factory = TransformerFactory.newInstance(); // Noncompliant
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, value);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, value);
    return factory;
  }

  TransformerFactory multiple_instances() throws TransformerConfigurationException {
    TransformerFactory factory = TransformerFactory.newInstance();
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    factory = TransformerFactory.newInstance(); // Noncompliant
    return factory;
  }

  // Directly used

  void directly_used() throws TransformerConfigurationException {
    TransformerFactory factory = TransformerFactory.newInstance(); // Noncompliant
    Transformer transformer = factory.newTransformer(new StreamSource("xxe.xml"));
  }

  void directly_used_no_args() throws TransformerConfigurationException {
    TransformerFactory factory = TransformerFactory.newInstance(); // Noncompliant
    Transformer transformer = factory.newTransformer();
  }

}

class TransformerFactoryCrossProceduralReturn {

  TransformerFactory cross_procedural() throws TransformerConfigurationException {
    TransformerFactory factory = TransformerFactory.newInstance();
    factory = enableSecureProcessing(factory);
    return factory;
  }

  TransformerFactory enableSecureProcessing(TransformerFactory factory) throws TransformerConfigurationException {
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    return factory;
  }
}

class TransformerFactoryCrossProceduralSideEffect1 {

  TransformerFactory cross_procedural() throws TransformerConfigurationException {
    TransformerFactory factory = TransformerFactory.newInstance();
    enableSecureProcessing(factory);
    return factory;
  }

  static void enableSecureProcessing(TransformerFactory factory) throws TransformerConfigurationException {
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
  }
}

class TransformerFactoryCrossProceduralSideEffect2 {

  TransformerFactory cross_procedural() throws TransformerConfigurationException {
    // FP, SE limiation, we can not know the runtime type of enableSecureProcessing if it's not final, static, ...
    TransformerFactory factory = TransformerFactory.newInstance(); // Noncompliant FP
    enableSecureProcessing(factory);
    return factory;
  }

  void enableSecureProcessing(TransformerFactory factory) throws TransformerConfigurationException {
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
  }
}
