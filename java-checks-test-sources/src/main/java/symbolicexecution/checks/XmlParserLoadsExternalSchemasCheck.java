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

  class NonCompliant {
    static DocumentBuilderFactory getDocumentBuilderFactory() throws ParserConfigurationException {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      // Securing against S2755
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setValidating(true); // We only report on the first location
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", true); // Noncompliant [[sc=7;ec=97]] {{Disable loading of external schemas in XML parsing.}}
      return factory;
    }

    static DocumentBuilderFactory getDocumentBuilderFactorySetValidatingTrueLast() throws ParserConfigurationException {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      // Securing against S2755
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", true);
      factory.setValidating(true); // Noncompliant [[sc=7;ec=34]] {{Disable loading of external schemas in XML parsing.}}
      return factory;
    }

    static SAXParserFactory getSaxParserFactory() throws SAXNotSupportedException, SAXNotRecognizedException, ParserConfigurationException {
      SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
      // Securing against S2755
      saxParserFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      saxParserFactory.setValidating(true);
      saxParserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", true); // Noncompliant {{Disable loading of external schemas in XML parsing.}}
      return saxParserFactory;
    }

    static SAXParserFactory getSaxParserFactorySetValidatingTrueLast() throws SAXNotSupportedException, SAXNotRecognizedException, ParserConfigurationException {
      SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
      // Securing against S2755
      saxParserFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      saxParserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", true);
      saxParserFactory.setValidating(true); // Noncompliant {{Disable loading of external schemas in XML parsing.}}
      return saxParserFactory;
    }

    static SchemaFactory getSchemaFactory() throws SAXNotSupportedException, SAXNotRecognizedException {
      SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      // Securing against S2755
      schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

      schemaFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", true); // Noncompliant {{Disable loading of external schemas in XML parsing.}}
      return schemaFactory;
    }

    static SAXReader getSAXReader() throws SAXException {
      // For https://dom4j.github.io/[Dom4j] library:
      SAXReader saxReader = new SAXReader();
      // Securing against S2755
      saxReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

      saxReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", true);  // Noncompliant {{Disable loading of external schemas in XML parsing.}}
      return saxReader;
    }

    static SAXBuilder getSaxBuilder() {
      // For http://www.jdom.org/[Jdom2] library:
      SAXBuilder saxBuilder = new SAXBuilder();
      // Securing against S2755
      saxBuilder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", true); // Noncompliant {{Disable loading of external schemas in XML parsing.}}
      return saxBuilder;
    }

    static DocumentBuilder getDocumentBuilderWithNullEntityResolver() throws ParserConfigurationException {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      // Securing against S2755
      dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

      dbf.setValidating(true); // Noncompliant {{Disable loading of external schemas in XML parsing.}}
      final DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
      documentBuilder.setEntityResolver(null); // No side-effect on the issue
      return documentBuilder;
    }

    static DocumentBuilder getDocumentBuilderWithNullEntityResolverLoadLast() throws ParserConfigurationException {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      // Securing against S2755
      dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", true); // Noncompliant {{Disable loading of external schemas in XML parsing.}}
      final DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
      documentBuilder.setEntityResolver(null); // No side-effect on the issue
      return documentBuilder;
    }

    static SAXBuilder getSaxBuilderWithNullEntityResolver() {
      SAXBuilder saxb = new SAXBuilder();
      // Securing against S2755
      saxb.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

      saxb.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", true); // Noncompliant {{Disable loading of external schemas in XML parsing.}}
      saxb.setEntityResolver(null); // No side-effect on the issue
      return saxb;
    }
  }

  class Compliant {

    static DocumentBuilderFactory getDocumentBuilderFactory() throws ParserConfigurationException {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      // Securing against S2755
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      return factory;
    }

    static SAXParserFactory getSaxParserFactory() throws SAXNotSupportedException, SAXNotRecognizedException, ParserConfigurationException {
      SAXParserFactory saxParserfactory = SAXParserFactory.newInstance();
      // Securing against S2755
      saxParserfactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

      saxParserfactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      return saxParserfactory;
    }

    static SchemaFactory getSchemaFactory() throws SAXNotSupportedException, SAXNotRecognizedException {
      SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      // Securing against S2755
      schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

      schemaFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      return schemaFactory;
    }

    static SAXReader getSaxReader() throws SAXException {
      // For https://dom4j.github.io/[Dom4j] library:
      SAXReader xmlReader = new SAXReader();
      // Securing against S2755
      xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      xmlReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      return xmlReader;
    }

    static SAXBuilder getSaxBuilder() {
      // For http://www.jdom.org/[Jdom2] library:
      SAXBuilder builder = new SAXBuilder();
      // Securing against S2755
      builder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

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
