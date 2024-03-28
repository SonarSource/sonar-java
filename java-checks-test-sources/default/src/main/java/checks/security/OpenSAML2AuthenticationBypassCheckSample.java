package checks.security;

import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.parse.StaticBasicParserPool;

class OpenSAML2AuthenticationBypassCheckSample {

  public void foo() {
    new StaticBasicParserPool().setIgnoreComments(false); // Noncompliant {{Change "setIgnoreComments" to "true" or remove the call to "setIgnoreComments" to prevent the authentication bypass.}}
    new StaticBasicParserPool().setIgnoreComments(true); // Compliant

    new BasicParserPool().setIgnoreComments(false); // Noncompliant {{Change "setIgnoreComments" to "true" or remove the call to "setIgnoreComments" to prevent the authentication bypass.}}
    new BasicParserPool().setIgnoreComments(true); // Compliant

    // OpenSAML3 is OK
    new net.shibboleth.utilities.java.support.xml.BasicParserPool().setIgnoreComments(false);
  }

}
