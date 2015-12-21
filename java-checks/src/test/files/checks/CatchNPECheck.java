class A {
  Object a;
  private void method(){
    try {
      a.equals(null);
    } catch(NullPointerException npe) { // Noncompliant [[sc=13;ec=33]] {{Avoid catching NullPointerException.}}
      log.info("argument was null");
    }
    try {
      a.equals(null);
    } catch(java.lang.NullPointerException npe) { // Noncompliant [[sc=23;ec=43]] {{Avoid catching NullPointerException.}}
      log.info("argument was null");
    }
    try {
      a.equals(null);
    } catch(java.lang.NullPointerException | RuntimeException runtimeException) { // Noncompliant {{Avoid catching NullPointerException.}}
      log.info("argument was null");
    }
  }
}
