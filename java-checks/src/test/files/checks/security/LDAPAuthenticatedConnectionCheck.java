import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.naming.Context;
import javax.xml.XMLConstants;

class S4433 {

  void method1() {
    Hashtable<String, String> env = new Hashtable<>();
    env.put(javax.naming.Context.SECURITY_AUTHENTICATION, "none"); // Noncompliant {{Change authentication to "simple" or stronger.}}
  }

  void method2() {
    Hashtable<String, String> env = new Hashtable<>();
    env.put(Context.SECURITY_AUTHENTICATION, "none"); // Noncompliant
    env.put(XMLConstants.FEATURE_SECURE_PROCESSING, "none");
  }

  void method3(java.util.Properties props) {
    String authMechanism = props.getProperty("AUTH_MECH", "none");
    Map<String, String> env = new Hashtable<>();
    env.put(Context.SECURITY_AUTHENTICATION, authMechanism); // Noncompliant
  }

  void method4() {
    Map<String, String> env = new ConcurrentHashMap<>();
    env.put("java.naming.security.authentication", "none"); // Noncompliant
  }

  void method5() {
    Hashtable<String, String> env = new Hashtable<>();
    env.put(Context.SECURITY_AUTHENTICATION, "simple");
  }

  void method6(java.util.Properties props) {
    String authMechanism = props.getProperty("AUTH_MECH", "CRAM-MD5");
    Hashtable<String, String> env = new Hashtable<>();
    env.put(Context.SECURITY_AUTHENTICATION, authMechanism);
  }

  void method7() {
    Hashtable<String, String> env = new Hashtable<>();
    env.put(Context.SECURITY_PRINCIPAL, "none");
  }

  void method8() {
    Hashtable<String, String> env = new Hashtable<>();
    env.put("SECURITY_AUTHENTICATION", "none");
  }
}
