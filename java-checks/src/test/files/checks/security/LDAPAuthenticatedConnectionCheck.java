class Main {
  void method1() {
    env.put(javax.naming.Context.SECURITY_AUTHENTICATION, "none"); // Noncompliant {{Change authentication to "simple" or stronger.}}
  }

  void method2() {
    env.put(Context.SECURITY_AUTHENTICATION, "none"); // Noncompliant {{Change authentication to "simple" or stronger.}}
  }

  void method3() {
    env.put(Context.SECURITY_AUTHENTICATION, "simple");
  }

  void method4() {
    env.put(Context.SECURITY_AUTHENTICATION, "CRAM-MD5");
  }

  void method5() {
    env.put(Context.SECURITY_PRINCIPAL, "none");
  }
}
