import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.XMLConstants;

class A {

  static final boolean BOOLEAN_TRUE = true;

  TransformerFactory classField = TransformerFactory.newInstance();

  TransformerFactory no_call_to_securing_method() {
    TransformerFactory factory = TransformerFactory.newInstance(); // Noncompliant
    return factory;
  }

  TransformerFactory no_call_to_sax_securing_method() {
    TransformerFactory factory = SAXTransformerFactory.newInstance(); // Noncompliant
    return factory;
  }

  // setFeature

  TransformerFactory secure_processing_true() {
    TransformerFactory factory = TransformerFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    return factory;
  }

  TransformerFactory secure_processing_false() {
    TransformerFactory factory = TransformerFactory.newInstance(); // Noncompliant
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
    return factory;
  }

  TransformerFactory secure_processing_true_with_literal() {
    TransformerFactory factory = TransformerFactory.newInstance();
    factory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
    return factory;
  }

  TransformerFactory setfeature_with_other_than_secure_processing() {
    TransformerFactory factory = TransformerFactory.newInstance(); // Noncompliant
    factory.setFeature(XMLConstants.ACCESS_EXTERNAL_DTD, true);
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
    TransformerFactory factory = TransformerFactory.newInstance(); // Noncompliant
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "xxx");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    return factory;
  }

  TransformerFactory setattribute_dtd_and_stylesheet_with_unknown_value(String value) {
    TransformerFactory factory = TransformerFactory.newInstance(); // Noncompliant
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, value);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, value);
    return factory;
  }
}

class Limitations {

  TransformerFactory multiple_instances() {
    TransformerFactory factory = TransformerFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory = TransformerFactory.newInstance(); // FN
    return factory;
  }

  TransformerFactory constant_boolean_value() {
    TransformerFactory factory = TransformerFactory.newInstance(); // Noncompliant (FP)
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, BOOLEAN_TRUE);
    return factory;
  }

  TransformerFactory cross_procedural() {
    TransformerFactory factory = TransformerFactory.newInstance(); // Noncompliant (FP, would require symbolic execution)
    enableSecureProcessing(factory);
    return factory;
  }

  void enableSecureProcessing(TransformerFactory factory) {
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
  }
}

interface I {
  Runnable r = () -> {
    TransformerFactory factory = TransformerFactory.newInstance();
  };
}
