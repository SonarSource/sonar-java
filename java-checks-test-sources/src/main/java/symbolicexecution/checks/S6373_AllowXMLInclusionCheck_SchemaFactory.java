package symbolicexecution.checks;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.SAXException;

public class S6373_AllowXMLInclusionCheck_SchemaFactory {

  Validator x_include_is_false_by_default() throws SAXException {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); // Compliant, xinclude is false by default
    schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

    Schema schema = schemaFactory.newSchema();
    Validator validator = schema.newValidator();
    return validator;
  }

  Validator x_include_feature_to_false() throws SAXException {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); // Compliant, xinclude is explicitly false
    schemaFactory.setFeature("http://apache.org/xml/features/xinclude", false);
    schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

    Schema schema = schemaFactory.newSchema();
    Validator validator = schema.newValidator();
    return validator;
  }

  Validator x_include_feature_to_true() throws SAXException {
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); // Noncompliant [[sc=49;ec=60]] {{Disable the inclusion of files in XML processing.}}
    schemaFactory.setFeature("http://apache.org/xml/features/xinclude", true);
    schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

    Schema schema = schemaFactory.newSchema();
    Validator validator = schema.newValidator();
    return validator;
  }

}
