class A {
  private void f() {
    new Byte("").toString(); // Non-Compliant
    new Short(0).toString(); // Non-Compliant
    new Integer(0).toString(); // Non-Compliant
    new Long(0).toString(); // Non-Compliant
    new Float(0).toString(); // Non-Compliant
    new Double(0).toString(); // Non-Compliant
    new Character('a').toString(); // Non-Compliant
    new Boolean(false).toString(); // Non-Compliant
    new Integer(0).toString(0); // Non-Compliant

    new RuntimeException("").toString(); // Compliant
    Integer.toString(0); // Compliant
    new Integer(0).getClass().toString(); // Compliant

    new int[0].toString(); // Compliant
    new Integer.Foo().toString(); // Compliant

    foo++; // Compliant
    new Integer(0).this; // Compliant
    (foo).toString(); // Compliant
  }
}
