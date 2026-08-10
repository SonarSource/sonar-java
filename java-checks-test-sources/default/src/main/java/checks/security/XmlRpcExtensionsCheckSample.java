package checks.security;

import org.apache.xmlrpc.XmlRpcConfigImpl;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;

class XmlRpcExtensionsCheckSample {

  private static final boolean EXTENSIONS_ENABLED = true;
  private static final boolean EXTENSIONS_DISABLED = false;

  void serverConfigNoncompliant() {
    XmlRpcServerConfigImpl config = new XmlRpcServerConfigImpl();
    config.setEnabledForExtensions(true); // Noncompliant {{Disable extensions on this Apache XML RPC configuration.}}
  }

  void clientConfigNoncompliant() {
    XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
    config.setEnabledForExtensions(true); // Noncompliant
  }

  void serverConfigWithOtherSettings() {
    XmlRpcServerConfigImpl config = new XmlRpcServerConfigImpl();
    config.setBasicEncoding("UTF-8");
    config.setEnabledForExtensions(true); // Noncompliant
  }

  void constantTrue() {
    XmlRpcServerConfigImpl config = new XmlRpcServerConfigImpl();
    config.setEnabledForExtensions(EXTENSIONS_ENABLED); // Noncompliant
  }

  void unknownVariable(boolean enable) {
    XmlRpcServerConfigImpl config = new XmlRpcServerConfigImpl();
    config.setEnabledForExtensions(enable); // Compliant - unknown value, not provably true
  }

  void parentType() {
    XmlRpcConfigImpl config = new XmlRpcServerConfigImpl();
    config.setEnabledForExtensions(true); // Noncompliant
  }

  // Compliant examples

  void serverConfigDefault() {
    XmlRpcServerConfigImpl config = new XmlRpcServerConfigImpl();
    // Extensions are disabled by default
  }

  void clientConfigDefault() {
    XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
    // Extensions are disabled by default
  }

  void explicitlyDisabled() {
    XmlRpcServerConfigImpl config = new XmlRpcServerConfigImpl();
    config.setEnabledForExtensions(false);
  }

  void clientExplicitlyDisabled() {
    XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
    config.setEnabledForExtensions(false);
  }

  void constantFalse() {
    XmlRpcServerConfigImpl config = new XmlRpcServerConfigImpl();
    config.setEnabledForExtensions(EXTENSIONS_DISABLED);
  }

  void unrelatedType() {
    UnrelatedConfig config = new UnrelatedConfig();
    config.setEnabledForExtensions(true);
  }
}

class UnrelatedConfig {
  void setEnabledForExtensions(boolean enabled) {
  }
}
