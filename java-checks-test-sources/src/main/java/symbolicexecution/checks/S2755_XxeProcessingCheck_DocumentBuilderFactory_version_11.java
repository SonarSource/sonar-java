package symbolicexecution.checks;

import javax.xml.parsers.DocumentBuilderFactory;

class S2755_XxeProcessingCheck_DocumentBuilderFactory_version_11 {
  // Secure with "setExpandEntityReferences"
  DocumentBuilderFactory secure_with_set_expand_entity_references_false() {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Noncompliant
    // Java version is set and < 13: this fix is not enough to secure the factory
    factory.setExpandEntityReferences(false);
    return factory;
  }

  DocumentBuilderFactory secure_with_set_expand_entity_references_true() {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Noncompliant
    factory.setExpandEntityReferences(true);
    return factory;
  }
}
