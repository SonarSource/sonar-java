package symbolicexecution.checks;

import java.io.StringReader;
import org.jdom2.input.SAXBuilder;
import org.xml.sax.InputSource;

public class S6373_AllowXMLInclusionCheck_SAXBuilder {

  SAXBuilder x_include_is_false_by_default() {
    SAXBuilder builder = new SAXBuilder(); // Compliant, xinclude is false by default
    builder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return builder;
  }

  SAXBuilder x_include_feature_to_false() {
    SAXBuilder builder = new SAXBuilder(); // Compliant, xinclude is explicitly false
    builder.setFeature("http://apache.org/xml/features/xinclude", false);
    builder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return builder;
  }

  SAXBuilder x_include_feature_to_true() {
    SAXBuilder builder = new SAXBuilder();
    builder.setFeature("http://apache.org/xml/features/xinclude", true); // Noncompliant {{Disable the inclusion of files in XML processing.}}
    builder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return builder;
  }

  SAXBuilder x_include_feature_to_true_without_entity_resolver() {
    SAXBuilder builder = new SAXBuilder();
    builder.setFeature("http://apache.org/xml/features/xinclude", true); // Noncompliant
    builder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    builder.setEntityResolver(null);
    return builder;
  }

  SAXBuilder x_include_feature_to_true_with_entity_resolver() {
    SAXBuilder builder = new SAXBuilder(); // Compliant with a custom entity resolver
    builder.setFeature("http://apache.org/xml/features/xinclude", true);
    builder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
    return builder;
  }

}
