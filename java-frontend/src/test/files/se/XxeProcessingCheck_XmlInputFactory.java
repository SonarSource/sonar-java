import javax.xml.stream.XMLInputFactory;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLEventReader;

class XMLInputFactoryTest {
  // Vulnerable when nothing is made to protect against xxe
  XMLInputFactory no_property_new_instance() {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Noncompliant [[sc=47;ec=58]] {{Disable XML external entity (XXE) processing.}}
    return factory;
  }

  XMLInputFactory no_property_new_factory() {
    XMLInputFactory factory = XMLInputFactory.newFactory(); // Noncompliant
    return factory;
  }

  XMLInputFactory unrelated_property() {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Noncompliant
    factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
    return factory;
  }

  XMLInputFactory other_call(Object value) {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Noncompliant
    factory.setXMLReporter(null);
    return factory;
  }

  // Not vulnerable when the support of external entities is disabled.
  XMLInputFactory dtd_with_false_value() {
    XMLInputFactory factory = XMLInputFactory.newInstance();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    return factory;
  }

  XMLInputFactory dtd_with_primitive_false() {
    XMLInputFactory factory = XMLInputFactory.newInstance();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    return factory;
  }

  XMLInputFactory dtd_with_primitive_false2() {
    XMLInputFactory factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    return factory;
  }

  XMLInputFactory dtd_with_primitive_true() {
    XMLInputFactory factory = XMLInputFactory.newFactory(); // Noncompliant
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, true);
    return factory;
  }

  XMLInputFactory dtd_with_primitive_true2() {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Noncompliant
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, true);
    return factory;
  }

  XMLInputFactory dtd_with_unknown_value(Object value) {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Noncompliant
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, value);
    return factory;
  }

  XMLInputFactory is_supporting_external_entities_false() {
    XMLInputFactory factory = XMLInputFactory.newInstance();
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, "false");
    return factory;
  }

  XMLInputFactory is_supporting_external_entities_true() {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Noncompliant
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, "true");
    return factory;
  }

  // "universal fix": ACCESS_EXTERNAL_DTD and ACCESS_EXTERNAL_SCHEMA should be set to ""
  XMLInputFactory setProperty_dtd_schema(Object value) {
    XMLInputFactory factory = XMLInputFactory.newInstance();
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return factory;
  }

  XMLInputFactory setProperty_dtd_schema_new_factory(Object value) {
    XMLInputFactory factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return factory;
  }

  XMLInputFactory setProperty_dtd_schema_with_literal(Object value) {
    XMLInputFactory factory = XMLInputFactory.newInstance();
    factory.setProperty("http://javax.xml.XMLConstants/property/accessExternalDTD", "");
    factory.setProperty("http://javax.xml.XMLConstants/property/accessExternalSchema", "");
    return factory;
  }

  XMLInputFactory setProperty_dtd_only(Object value) {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Noncompliant
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    return factory;
  }

  XMLInputFactory setProperty_schema_only(Object value) {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Noncompliant
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return factory;
  }

  XMLInputFactory setattribute_dtd_and_other(Object value) {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Noncompliant
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setProperty(XMLConstants.DEFAULT_NS_PREFIX, "");
    return factory;
  }

  XMLInputFactory setProperty_dtd_schema_non_empty(Object value) {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Noncompliant
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "all");
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return factory;
  }

  XMLInputFactory setProperty_dtd_schema_unknown(String value) {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Noncompliant
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, value);
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, value);
    return factory;
  }

  // Detect issues depending on the symbol
  XMLInputFactory two_factory(boolean b) {
    if (b) {
      XMLInputFactory factory = XMLInputFactory.newInstance(); // Noncompliant
      return factory;
    } else {
      XMLInputFactory factory = XMLInputFactory.newInstance();
      factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, "false");
      return factory;
    }
  }

  XMLInputFactory two_factory_assign(boolean b) {
    if (b) {
      XMLInputFactory factory;
      factory = XMLInputFactory.newInstance(); // Noncompliant
      return factory;
    } else {
      XMLInputFactory factory;
      factory = XMLInputFactory.newInstance();
      factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, "false");
      return factory;
    }
  }
}

class XMLInputFactoryDirect {
  void factory_call_directly(java.io.Reader reader) throws Exception {
    XMLEventReader eventReader = XMLInputFactory.newInstance().createXMLEventReader(reader); // Noncompliant
  }

  void factory_correctly_used(java.io.Reader reader) throws XMLStreamException {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Compliant, used and secured
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    XMLEventReader eventReader = factory.createXMLEventReader(reader);
  }

  void factory_unused() {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Compliant, unused
  }

  void factory_not_secured_used(java.io.Reader reader) throws XMLStreamException {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Noncompliant
    XMLEventReader eventReader = factory.createXMLEventReader(reader);
  }

  XMLInputFactory factory_not_secured_used_and_return(java.io.Reader reader) throws XMLStreamException {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Noncompliant
    XMLEventReader eventReader1 = factory.createXMLEventReader(reader);
    return factory;
  }

  void factory_used_as_parameter(java.io.Reader reader) throws XMLStreamException {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Compliant, we loose track of factory
    using_factory(factory);
  }

  void using_factory(XMLInputFactory factory) {
    // Do something with factory...
  }

  void factory_used_as_parameter_for_securing(java.io.Reader reader) throws XMLStreamException {
    XMLInputFactory factory = XMLInputFactory.newInstance(); // Compliant
    securing(factory);
  }

  void securing(XMLInputFactory factory) {
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
  }

}

class XMLInputFactoryTest2 {
  // Test support with class fields.
  XMLInputFactory factory = XMLInputFactory.newInstance(); // Compliant, reported only when declared inside method

  XMLInputFactory factory_instance() {
    XMLInputFactoryTest2 staxTest = new XMLInputFactoryTest2();
    staxTest.factory = XMLInputFactory.newInstance(); // Compliant
    staxTest.setFactoryProperty();
    return staxTest.factory;
  }

  XMLInputFactory factory_field() {
    factory = XMLInputFactory.newInstance(); // Compliant
    return factory;
  }

  XMLInputFactory factory_field2() {
    factory = XMLInputFactory.newInstance(); // Compliant
    this.factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, "false");
    return this.factory;
  }

  XMLInputFactory factory_field3() {
    this.factory = XMLInputFactory.newInstance(); // Compliant
    this.factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, "false");
    return this.factory;
  }

  XMLInputFactory factory_field4() {
    this.factory = XMLInputFactory.newInstance(); // Compliant
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, "false");
    return factory;
  }

  XMLInputFactory factory_field5() {
    factory = XMLInputFactory.newInstance(); // Compliant
    setFactoryProperty();
    return factory;
  }

  XMLInputFactory factory_field6() {
    factory = XMLInputFactory.newInstance(); // Compliant
    setFactoryProperty(factory);
    return factory;
  }

  private void setFactoryProperty() {
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, "false");
  }

  private static void setFactoryProperty(XMLInputFactory factory) {
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, "false");
  }
}
