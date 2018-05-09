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

  void method3() {
    Hashtable<String, String> env = new Hashtable<>();
    env.put(Context.SECURITY_AUTHENTICATION, "simple");
  }

  void method4() {
    Hashtable<String, String> env = new Hashtable<>();
    env.put(Context.SECURITY_AUTHENTICATION, "CRAM-MD5");
  }

  void method5() {
    Hashtable<String, String> env = new Hashtable<>();
    env.put(Context.SECURITY_PRINCIPAL, "none");
  }
}
