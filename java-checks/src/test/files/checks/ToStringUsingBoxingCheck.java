class A {
  private void f() {
    new Byte("").toString(); // Noncompliant [[sc=5;ec=17]] {{Call the static method Byte.toString(...) instead of instantiating a temporary object to perform this to string conversion.}}
    new Short(0).toString(); // Noncompliant {{Call the static method Short.toString(...) instead of instantiating a temporary object to perform this to string conversion.}}
    new Integer(0).toString(); // Noncompliant {{Call the static method Integer.toString(...) instead of instantiating a temporary object to perform this to string conversion.}}
    new Long(0).toString(); // Noncompliant {{Call the static method Long.toString(...) instead of instantiating a temporary object to perform this to string conversion.}}
    new Float(0).toString(); // Noncompliant {{Call the static method Float.toString(...) instead of instantiating a temporary object to perform this to string conversion.}}
    new Double(0).toString(); // Noncompliant {{Call the static method Double.toString(...) instead of instantiating a temporary object to perform this to string conversion.}}
    new Character('a').toString(); // Noncompliant {{Call the static method Character.toString(...) instead of instantiating a temporary object to perform this to string conversion.}}
    new Boolean(false).toString(); // Noncompliant {{Call the static method Boolean.toString(...) instead of instantiating a temporary object to perform this to string conversion.}}
    new Integer(0).toString(0); // Noncompliant {{Call the static method Integer.toString(...) instead of instantiating a temporary object to perform this to string conversion.}}
    new Integer(0).compareTo(0); // Noncompliant {{Call the static method Integer.compare(...) instead of instantiating a temporary object to perform this to string conversion.}}
    new Boolean(false).compareTo(true); // Noncompliant {{Call the static method Boolean.compare(...) instead of instantiating a temporary object to perform this to string conversion.}}

    new RuntimeException("").toString(); // Compliant
    Integer.toString(0); // Compliant
    new Integer(0).getClass().toString(); // Compliant

    new int[0].toString(); // Compliant
    new Integer.Foo().toString(); // Compliant

    foo++; // Compliant
    (foo).toString(); // Compliant
    foo();
  }
}
