package symbolicexecution.checks;

import javax.xml.crypto.KeySelector;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import org.w3c.dom.Node;

abstract class S3677_XmlValidatedSignatureCheck {
  private final XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
  private DOMValidateContext valContextField;
  private Object field;

  void compliant_1(Node node, KeySelector selector) throws Exception {
    DOMValidateContext valContext = new DOMValidateContext(selector, node);
    valContext.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.TRUE); // Compliant

    XMLSignature signature = fac.unmarshalXMLSignature(valContext);

    signature.validate(valContext);
  }

  void compliant_2(Node node, KeySelector selector) throws Exception {
    valContextField = new DOMValidateContext(selector, node);
    valContextField.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.FALSE); // Compliant - field...

    XMLSignature signature2 = fac.unmarshalXMLSignature(valContextField);
    signature2.validate(valContextField);
  }

  void compliant_3(Node node, KeySelector selector) throws Exception {
    DOMValidateContext valContext = new DOMValidateContext(selector, node);
    valContext.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.FALSE);
    valContext.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.TRUE);

    XMLSignature signature = fac.unmarshalXMLSignature(valContext);

    signature.validate(valContext);
  }

  void compliant_4(Node node, KeySelector selector) throws Exception {
    DOMValidateContext valContext = new DOMValidateContext(selector, node);
    valContext.setProperty("org.jcp.xml.dsig.secureValidation", foo()); // Compliant - no idea of what it could be

    XMLSignature signature = fac.unmarshalXMLSignature(valContext);

    signature.validate(valContext);
  }

  private void coverage(DOMValidateContext valContext) {
    field = new Object();

    Object[] arr = new Object[1];
    arr[0] = field;

    valContext.setProperty("some.other.feature", field);
  }

  abstract boolean foo();

  void noncompliant_1(Node node, KeySelector selector) throws Exception {
    DOMValidateContext valContext = new DOMValidateContext(selector, node);
    valContext.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.FALSE); // Noncompliant {{Change this to "true" to validate this XML signature securely.}}
//                                                              ^^^^^^^^^^^^^

    XMLSignature signature = fac.unmarshalXMLSignature(valContext);

    signature.validate(valContext);
  }

  void noncompliant_2(Node node, KeySelector selector) throws Exception {
    DOMValidateContext valContext = new DOMValidateContext(selector, node); // Noncompliant {{Set the 'org.jcp.xml.dsig.secureValidation' property to "true" on the 'DOMValidateContext' object to validate this XML signature securely.}}

    XMLSignature signature = fac.unmarshalXMLSignature(valContext);

    signature.validate(valContext);
  }

  void noncompliant_3(Node node, KeySelector selector) throws Exception {
    DOMValidateContext valContext = new DOMValidateContext(selector, node);
    valContext.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.TRUE);
    valContext.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.FALSE); // Noncompliant {{Change this to "true" to validate this XML signature securely.}}
//                                                              ^^^^^^^^^^^^^

    XMLSignature signature = fac.unmarshalXMLSignature(valContext);

    signature.validate(valContext);
  }

  DOMValidateContext noncompliant_4(Node node, KeySelector selector) throws Exception {
    DOMValidateContext valContext = new DOMValidateContext(selector, node);
    valContext.setProperty("org.jcp.xml.dsig.secureValidation", false); // Noncompliant {{Change this to "true" to validate this XML signature securely.}}
//                                                              ^^^^^

    return valContext;
  }

  /**
   * {@link XMLSignature.SignatureValue#validate(javax.xml.crypto.dsig.XMLValidateContext)}
   */
  void noncompliant_5(Node node, KeySelector selector) throws Exception {
    DOMValidateContext valContext1 = new DOMValidateContext(selector, node); // Noncompliant
    XMLSignature.SignatureValue signatureValue1 = fac.unmarshalXMLSignature(valContext1).getSignatureValue();
    signatureValue1.validate(valContext1);

    DOMValidateContext valContext2 = new DOMValidateContext(selector, node); // Compliant
    valContext2.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.TRUE);
    XMLSignature.SignatureValue signatureValue2 = fac.unmarshalXMLSignature(valContext2).getSignatureValue();
    signatureValue2.validate(valContext2);
  }

  /**
   * {@link Reference#validate(javax.xml.crypto.dsig.XMLValidateContext)}
   */
  void noncompliant_6(Node node, KeySelector selector, Reference dom) throws Exception {
    DOMValidateContext valContext1 = new DOMValidateContext(selector, node); // Noncompliant
    dom.validate(valContext1);

    DOMValidateContext valContext2 = new DOMValidateContext(selector, node); // Compliant
    valContext2.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.TRUE);
    dom.validate(valContext2);
  }

}
