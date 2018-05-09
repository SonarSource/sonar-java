import java.util.Hashtable;
import javax.naming.Context;

class S4433 {

  void method1() {
    Hashtable<String, String> env = new Hashtable<>();
    env.put(javax.naming.Context.SECURITY_AUTHENTICATION, "none"); // Noncompliant {{Change authentication to "simple" or stronger.}}
  }

  void method2() {
    Hashtable<String, String> env = new Hashtable<>();
    env.put(Context.SECURITY_AUTHENTICATION, "none"); // Noncompliant {{Change authentication to "simple" or stronger.}}
  }

  void method3(java.util.Properties props) {
    String authMechanism = props.getProperty("AUTH_MECH", "none");
    Hashtable<String, String> env = new Hashtable<>();
    env.put(Context.SECURITY_AUTHENTICATION, authMechanism); // Noncompliant {{Change authentication to "simple" or stronger.}}
  }

  void method4() {
    Hashtable<String, String> env = new Hashtable<>();
    env.put(Context.SECURITY_AUTHENTICATION, "simple");
  }

  void method5() {
    Hashtable<String, String> env = new Hashtable<>();
    env.put(Context.SECURITY_AUTHENTICATION, "CRAM-MD5");
  }

  void method6(java.util.Properties props) {
    String authMechanism = props.getProperty("AUTH_MECH", "simple");
    Hashtable<String, String> env = new Hashtable<>();
    env.put(Context.SECURITY_PRINCIPAL, authMechanism);
  }

  void method7() {
    Hashtable<String, String> env = new Hashtable<>();
    env.put(Context.SECURITY_PRINCIPAL, "none");
  }
}
