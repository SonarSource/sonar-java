package symbolicexecution.checks;

import java.io.IOException;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.SchemaFactory;
import org.dom4j.io.SAXReader;
import org.jdom2.input.SAXBuilder;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

public class XmlParserLoadsExternalSchemasCheck {
  public static final String ATTRIBUTE_NAME = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

  class NonCompliant {
    static DocumentBuilderFactory getDocumentBuilderFactory() throws ParserConfigurationException {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Noncompliant
      factory.setValidating(true);
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", true); // Noncompliant
      return factory;
    }

    static SAXParserFactory getSaxParserFactory() throws SAXNotSupportedException, SAXNotRecognizedException, ParserConfigurationException {
      SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
      saxParserFactory.setValidating(true); // Noncompliant
      saxParserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", true); // Noncompliant
      return saxParserFactory;
    }

    static SchemaFactory getSchemaFactory() throws SAXNotSupportedException, SAXNotRecognizedException {
      SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      schemaFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", true); // Noncompliant
      return schemaFactory;
    }

    static SAXReader getSAXReader() throws SAXException {
      // For https://dom4j.github.io/[Dom4j] library:
      SAXReader saxReader = new SAXReader(); // Noncompliant
      saxReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", true);  // Noncompliant
      return saxReader;
    }

    static SAXBuilder getSaxBuilder() {
      // For http://www.jdom.org/[Jdom2] library:
      SAXBuilder saxBuilder = new SAXBuilder();
      saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", true); // Noncompliant
      return saxBuilder;
    }

    static DocumentBuilder getDocumentBuilderWithNullEntityResolver() throws ParserConfigurationException {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setValidating(true);
      final DocumentBuilder documentBuilder = dbf.newDocumentBuilder(); // Noncompliant
      documentBuilder.setEntityResolver(null);
      return documentBuilder;
    }

    static SAXBuilder getSaxBuilderWithNullEntityResolver() {
      SAXBuilder saxb = new SAXBuilder(); // Noncompliant
      saxb.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", true);
      saxb.setEntityResolver(null);
      return saxb;
    }
  }

  class Compliant {

    static DocumentBuilderFactory getDocumentBuilderFactory() throws ParserConfigurationException {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      return factory;
    }

    static SAXParserFactory getSaxParserFactory() throws SAXNotSupportedException, SAXNotRecognizedException, ParserConfigurationException {
      SAXParserFactory saxParserfactory = SAXParserFactory.newInstance();
      saxParserfactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      return saxParserfactory;
    }

    static SchemaFactory getSchemaFactory() throws SAXNotSupportedException, SAXNotRecognizedException {
      SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      schemaFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      return schemaFactory;
    }

    static SAXReader getSaxReader() throws SAXException {
      // For https://dom4j.github.io/[Dom4j] library:
      SAXReader xmlReader = new SAXReader(); // Noncompliant
      xmlReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      return xmlReader;
    }

    static SAXBuilder getSaxBuilder() {
      // For http://www.jdom.org/[Jdom2] library:
      SAXBuilder builder = new SAXBuilder();
      builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      return builder;
    }

    static DocumentBuilder getDocumentBuilderWithEntityResolver() throws ParserConfigurationException {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setValidating(true);
      final DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
      documentBuilder.setEntityResolver(new EmptyEntityResolver());
      return documentBuilder;
    }

    static SAXBuilder getSaxBuilderWithEntityResolver() {
      SAXBuilder saxb = new SAXBuilder();
      saxb.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", true);
      saxb.setEntityResolver(new EmptyEntityResolver());
      return saxb;
    }
  }

  private static class EmptyEntityResolver implements EntityResolver {

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
      return null;
    }
  }
}
