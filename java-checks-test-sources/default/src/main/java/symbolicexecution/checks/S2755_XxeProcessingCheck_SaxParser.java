package symbolicexecution.checks;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.SchemaFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import javax.xml.XMLConstants;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import java.io.IOException;
import java.io.InputStream;

class SAXParserTest {
  // Vulnerable when nothing is made to protect against xxe
  SAXParserFactory no_property() {
    SAXParserFactory factory = SAXParserFactory.newInstance(); // Noncompliant {{Disable access to external entities in XML parsing.}}
    return factory;
  }

  // Not vulnerable when "http://apache.org/xml/features/disallow-doctype-decl" set to true
  // or http://xml.org/sax/features/external-general-entities set to false
  SAXParserFactory dissalow_doctype_set_to_true() throws SAXNotSupportedException, SAXNotRecognizedException, ParserConfigurationException {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return factory;
  }

  SAXParserFactory dissalow_doctype_set_to_false() throws SAXNotSupportedException, SAXNotRecognizedException, ParserConfigurationException {
    SAXParserFactory factory = SAXParserFactory.newInstance(); // Noncompliant
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
    return factory;
  }

  SAXParserFactory extrenal_general_property_set_to_true() throws SAXNotSupportedException, SAXNotRecognizedException, ParserConfigurationException {
    SAXParserFactory factory = SAXParserFactory.newInstance(); // Noncompliant
    factory.setFeature("http://xml.org/sax/features/external-general-entities", true);
    return factory;
  }

  SAXParserFactory extrenal_general_property_set_to_false() throws SAXNotSupportedException, SAXNotRecognizedException, ParserConfigurationException {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    return factory;
  }

  SAXParserFactory secure_processing_set_to_false() throws SAXNotSupportedException, SAXNotRecognizedException, ParserConfigurationException {
    SAXParserFactory factory = SAXParserFactory.newInstance(); // Noncompliant
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true); // No effect
    return factory;
  }

  SAXParserFactory other_feature_set_to_true() throws SAXNotSupportedException, SAXNotRecognizedException, ParserConfigurationException {
    SAXParserFactory factory = SAXParserFactory.newInstance(); // Noncompliant
    factory.setFeature("xxx", true);
    return factory;
  }

  SAXParserFactory other_call() throws SAXNotSupportedException, SAXNotRecognizedException, ParserConfigurationException {
    SAXParserFactory factory = SAXParserFactory.newInstance(); // Noncompliant
    factory.setValidating(true);
    return factory;
  }

  // "universal fix": ACCESS_EXTERNAL_DTD and ACCESS_EXTERNAL_SCHEMA should be set to ""
  SAXParserFactory univeral_fix() throws SAXException, ParserConfigurationException {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    SAXParser parser = factory.newSAXParser();
    parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return factory;
  }

  SAXParserFactory univeral_fix_only_dtd() throws SAXException, ParserConfigurationException {
    SAXParserFactory factory = SAXParserFactory.newInstance(); // Noncompliant
    SAXParser parser = factory.newSAXParser();
    parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    return factory;
  }

  SAXParserFactory univeral_fix_only_schema() throws SAXException, ParserConfigurationException {
    SAXParserFactory factory = SAXParserFactory.newInstance(); // Noncompliant
    SAXParser parser = factory.newSAXParser();
    parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); // compliant
    return factory;
  }

  SAXParserFactory univeral_fix_not_empty() throws SAXException, ParserConfigurationException {
    SAXParserFactory factory = SAXParserFactory.newInstance(); // Noncompliant
    SAXParser parser = factory.newSAXParser();
    parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "all");
    parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "all");
    return factory;
  }

  SAXParserFactory univeral_fix_not_enough_but_entities_disabled() throws SAXException, ParserConfigurationException {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    SAXParser parser = factory.newSAXParser();
    parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return factory;
  }

  SAXParserFactory parser_is_a_parameter(SAXParser parser) throws SAXException, ParserConfigurationException {
    SAXParserFactory factory = SAXParserFactory.newInstance(); // Noncompliant
    parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return factory;
  }

  // XMLReader from SAXParser

  XMLReader xml_reader_from_sax_parser() throws ParserConfigurationException, SAXException, IOException {
    SAXParserFactory factory = SAXParserFactory.newInstance(); // Noncompliant
    SAXParser parser = factory.newSAXParser();
    XMLReader xmlReader = parser.getXMLReader();
    return xmlReader;
  }

  XMLReader xml_reader_from_sax_parser_secured() throws ParserConfigurationException, SAXException, IOException {
    SAXParserFactory factory = SAXParserFactory.newInstance(); // Compliant
    SAXParser parser = factory.newSAXParser();
    XMLReader xmlReader = parser.getXMLReader();
    xmlReader.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    xmlReader.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return xmlReader;
  }

  XMLReader xml_reader_from_sax_parser_secured_2() throws ParserConfigurationException, SAXException, IOException {
    SAXParserFactory factory = SAXParserFactory.newInstance(); // Compliant
    SAXParser parser = factory.newSAXParser();
    XMLReader xmlReader = parser.getXMLReader();
    xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return xmlReader;
  }

  // Not returned but used

  void used_not_returned() throws ParserConfigurationException, SAXException, IOException {
    DefaultHandler handler = new DefaultHandler();
    SAXParserFactory factory = SAXParserFactory.newInstance(); // Noncompliant
    SAXParser parser = factory.newSAXParser();
    parser.parse("xxe.xml", handler);
  }

  void used_not_returned_3_args(InputStream inputStream) throws ParserConfigurationException, SAXException, IOException {
    DefaultHandler handler = new DefaultHandler();
    SAXParserFactory factory = SAXParserFactory.newInstance(); // Noncompliant
    SAXParser parser = factory.newSAXParser();
    parser.parse("xxe.xml", handler);
    parser.parse(inputStream, handler, "a");
  }

  void xml_reader_from_sax_parser_used() throws ParserConfigurationException, SAXException, IOException {
    SAXParserFactory factory = SAXParserFactory.newInstance(); // Noncompliant
    SAXParser parser = factory.newSAXParser();
    XMLReader xmlReader = parser.getXMLReader();
    xmlReader.parse("xxe.xml");
  }

  void correctly_secured_in_try_catch(InputSource inputSource) throws SAXException, ParserConfigurationException {
    try {
      DefaultHandler handler = new DefaultHandler();
      SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();

      SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      saxParserFactory.setSchema(schemaFactory.newSchema(new SAXSource(inputSource)));

      SAXParser parser = saxParserFactory.newSAXParser();
      parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      parser.parse("xxe.xml", handler);
    } catch(Exception e) {
      // Do nothing
    }
  }

  // Detect issues depending on the symbol
  SAXParserFactory two_factory(boolean b) throws SAXNotSupportedException, SAXNotRecognizedException, ParserConfigurationException {
    if (b) {
      SAXParserFactory factory = SAXParserFactory.newInstance(); // Noncompliant
      return factory;
    } else {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      return factory;
    }
  }
}
